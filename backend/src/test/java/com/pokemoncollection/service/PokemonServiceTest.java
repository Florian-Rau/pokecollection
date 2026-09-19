package com.pokemoncollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokemoncollection.dto.PokemonPageDto;
import com.pokemoncollection.dto.PokemonSummaryDto;
import com.pokemoncollection.exception.PokeApiUnavailableException;
import com.pokemoncollection.exception.PokemonNotFoundException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class PokemonServiceTest
{
  private HttpServer server;
  private StubPokeApi stubPokeApi;
  private PokemonService pokemonService;

  @BeforeEach
  void setUp() throws IOException
  {
    stubPokeApi = new StubPokeApi();
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/api/v2", stubPokeApi::handle);
    server.start();

    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(3000);
    requestFactory.setReadTimeout(3000);

    RestClient restClient = RestClient.builder()
      .baseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v2")
      .requestFactory(requestFactory)
      .build();

    pokemonService = new PokemonService(restClient);
  }

  @AfterEach
  void tearDown()
  {
    server.stop(0);
  }

  @Test
  void getPageMapsListResponsesIntoPokemonSummaries()
  {
    stubPokeApi.respondWithJson("""
                                  {
                                    "count": 1302,
                                    "next": "https://pokeapi.co/api/v2/pokemon?offset=20&limit=20",
                                    "previous": null,
                                    "results": [
                                      {"name": "bulbasaur", "url": "https://pokeapi.co/api/v2/pokemon/1/"},
                                      {"name": "ivysaur", "url": "https://pokeapi.co/api/v2/pokemon/2/"}
                                    ]
                                  }
                                  """);

    PokemonPageDto page = pokemonService.getPage(20, 0);

    assertThat(page.count()).isEqualTo(1302);
    assertThat(page.next()).isEqualTo("https://pokeapi.co/api/v2/pokemon?offset=20&limit=20");
    assertThat(page.previous()).isNull();
    assertThat(page.results()).containsExactly(
      new PokemonSummaryDto(
        1L,
        "bulbasaur",
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png"
      ),
      new PokemonSummaryDto(
        2L,
        "ivysaur",
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/2.png"
      )
    );
    assertThat(stubPokeApi.lastRequestPath()).isEqualTo("/api/v2/pokemon");
    assertThat(stubPokeApi.lastRequestQuery()).isEqualTo("limit=20&offset=0");
  }

  @Test
  void getPageTreatsMalformedResponsesAsCatalogUnavailability()
  {
    stubPokeApi.respondWithJson("""
                                  {
                                    "count": 1302,
                                    "next": null,
                                    "previous": null,
                                    "results": [
                                      {"name": "bulbasaur", "url": "https://pokeapi.co/api/v2/not-pokemon/1/"}
                                    ]
                                  }
                                  """);

    assertThatThrownBy(() -> pokemonService.getPage(20, 0))
      .isInstanceOf(PokeApiUnavailableException.class)
      .hasMessage("Der Pokémon-Katalog ist vorübergehend nicht verfügbar. Bitte versuche es später erneut.");
  }

  @Test
  void getPageTreatsSlowResponsesAsCatalogUnavailability()
  {
    stubPokeApi.respondWithDelayedJson(3100, """
      {
        "count": 1302,
        "next": null,
        "previous": null,
        "results": []
      }
      """);

    assertThatThrownBy(() -> pokemonService.getPage(20, 0))
      .isInstanceOf(PokeApiUnavailableException.class)
      .hasMessage("Der Pokémon-Katalog ist vorübergehend nicht verfügbar. Bitte versuche es später erneut.");
  }

  @Test
  void findByNameReturnsTheRequestedPokemon()
  {
    stubPokeApi.respondWithJson("""
                                  {
                                    "id": 25,
                                    "name": "pikachu"
                                  }
                                  """);

    PokemonSummaryDto pokemon = pokemonService.findByName("Pikachu");

    assertThat(pokemon).isEqualTo(new PokemonSummaryDto(
      25L,
      "pikachu",
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png"
    ));
    assertThat(stubPokeApi.lastRequestPath()).isEqualTo("/api/v2/pokemon/pikachu");
  }

  @Test
  void findByIdReturnsTheRequestedPokemon()
  {
    stubPokeApi.respondWithJson("""
                                  {
                                    "id": 25,
                                    "name": "pikachu"
                                  }
                                  """);

    PokemonSummaryDto pokemon = pokemonService.findById(25L);

    assertThat(pokemon).isEqualTo(new PokemonSummaryDto(
      25L,
      "pikachu",
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png"
    ));
    assertThat(stubPokeApi.lastRequestPath()).isEqualTo("/api/v2/pokemon/25");
  }

  @Test
  void findByIdTreatsMalformedResponsesAsCatalogUnavailability()
  {
    stubPokeApi.respondWithJson("""
                                  {
                                    "id": null,
                                    "name": null
                                  }
                                  """);

    assertThatThrownBy(() -> pokemonService.findById(25L))
      .isInstanceOf(PokeApiUnavailableException.class)
      .hasMessage("Der Pokémon-Katalog ist vorübergehend nicht verfügbar. Bitte versuche es später erneut.");
  }

  @Test
  void findByNameSurfacesNotFoundAsADistinctException()
  {
    stubPokeApi.respondWithStatus(404, """
      {
        "detail": "Not found."
      }
      """);

    assertThatThrownBy(() -> pokemonService.findByName("missingno"))
      .isInstanceOf(PokemonNotFoundException.class)
      .hasMessage("Kein Pokémon mit dem Namen 'missingno' gefunden.");
  }

  @Test
  void findByIdSurfacesNotFoundAsADistinctException()
  {
    stubPokeApi.respondWithStatus(404, """
      {
        "detail": "Not found."
      }
      """);

    assertThatThrownBy(() -> pokemonService.findById(9999L))
      .isInstanceOf(PokemonNotFoundException.class)
      .hasMessage("Kein Pokémon mit der ID '9999' gefunden.");
  }

  @Test
  void findByNameTreatsSlowResponsesAsCatalogUnavailability()
  {
    stubPokeApi.respondWithDelayedJson(3100, """
      {
        "id": 25,
        "name": "pikachu"
      }
      """);

    assertThatThrownBy(() -> pokemonService.findByName("pikachu"))
      .isInstanceOf(PokeApiUnavailableException.class)
      .hasMessage("Der Pokémon-Katalog ist vorübergehend nicht verfügbar. Bitte versuche es später erneut.");
  }

  @Test
  void findByIdTreatsSlowResponsesAsCatalogUnavailability()
  {
    stubPokeApi.respondWithDelayedJson(3100, """
      {
        "id": 25,
        "name": "pikachu"
      }
      """);

    assertThatThrownBy(() -> pokemonService.findById(25L))
      .isInstanceOf(PokeApiUnavailableException.class)
      .hasMessage("Der Pokémon-Katalog ist vorübergehend nicht verfügbar. Bitte versuche es später erneut.");
  }

  private static final class StubPokeApi
  {

    private final AtomicReference<ResponseDefinition> response = new AtomicReference<>();
    private volatile String lastRequestPath;
    private volatile String lastRequestQuery;

    void respondWithJson(String body)
    {
      response.set(new ResponseDefinition(200, body, 0));
    }

    void respondWithDelayedJson(long delayMillis, String body)
    {
      response.set(new ResponseDefinition(200, body, delayMillis));
    }

    void respondWithStatus(int status, String body)
    {
      response.set(new ResponseDefinition(status, body, 0));
    }

    String lastRequestPath()
    {
      return lastRequestPath;
    }

    String lastRequestQuery()
    {
      return lastRequestQuery;
    }

    void handle(HttpExchange exchange) throws IOException
    {
      lastRequestPath = exchange.getRequestURI().getPath();
      lastRequestQuery = exchange.getRequestURI().getQuery();
      ResponseDefinition currentResponse = Objects.requireNonNull(response.get(), "No stub response configured.");

      if (currentResponse.delayMillis() > 0)
      {
        try
        {
          Thread.sleep(currentResponse.delayMillis());
        }
        catch (InterruptedException exception)
        {
          Thread.currentThread().interrupt();
        }
      }

      byte[] body = currentResponse.body().getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(currentResponse.status(), body.length);
      try (OutputStream outputStream = exchange.getResponseBody())
      {
        outputStream.write(body);
      }
    }

    private record ResponseDefinition(int status, String body, long delayMillis)
    {
    }
  }
}
