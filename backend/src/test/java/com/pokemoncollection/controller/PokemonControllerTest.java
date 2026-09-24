package com.pokemoncollection.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.pokemoncollection.entity.Trainer;
import com.pokemoncollection.repository.CollectionEntryRepository;
import com.pokemoncollection.repository.TrainerRepository;
import com.pokemoncollection.service.AuthService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class PokemonControllerTest
{
  private static HttpServer server;
  private static StubPokeApi stubPokeApi;

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext applicationContext;

  @Autowired
  private TrainerRepository trainerRepository;

  @Autowired
  private CollectionEntryRepository collectionEntryRepository;

  @Autowired
  private AuthService authService;

  @AfterAll
  static void stopServer()
  {
    if (server != null)
    {
      server.stop(0);
    }
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) throws IOException
  {
    ensureServerStarted();
    registry.add("pokeapi.base-url", () -> "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v2");
  }

  @BeforeEach
  void setUp()
  {
    mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
      .apply(springSecurity())
      .build();
    collectionEntryRepository.deleteAll();
    trainerRepository.deleteAll();
    trainerRepository.save(new Trainer("Chase", "hash"));
    stubPokeApi.reset();
  }

  @Test
  void getPageReturnsTheMappedPokemonCatalogPage() throws Exception
  {
    stubPokeApi.respondWith(200, """
      {
        "count": 1302,
        "next": "https://pokeapi.co/api/v2/pokemon?offset=20&limit=20",
        "previous": null,
        "results": [
          {"name": "bulbasaur", "url": "https://pokeapi.co/api/v2/pokemon/1/"}
        ]
      }
      """, 0);

    mockMvc.perform(get("/api/pokemon").header("Authorization", bearerToken()))
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/json"))
      .andExpect(jsonPath("$.count", equalTo(1302)))
      .andExpect(jsonPath("$.next", equalTo("https://pokeapi.co/api/v2/pokemon?offset=20&limit=20")))
      .andExpect(jsonPath("$.previous", nullValue()))
      .andExpect(jsonPath("$.results[0].pokemonId", equalTo(1)))
      .andExpect(jsonPath("$.results[0].name", equalTo("bulbasaur")))
      .andExpect(jsonPath(
        "$.results[0].spriteUrl",
        equalTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png")
      ));
  }

  @Test
  void getPageReturnsBadGatewayWhenPokeApiTimesOut() throws Exception
  {
    stubPokeApi.respondWith(200, """
      {
        "count": 1302,
        "next": null,
        "previous": null,
        "results": []
      }
      """, 3100);

    mockMvc.perform(get("/api/pokemon").header("Authorization", bearerToken()))
      .andExpect(status().isBadGateway())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Der Pokémon-Katalog ist vorübergehend nicht verfügbar. Bitte versuche es später erneut.")));
  }

  @Test
  void getPageReturnsBadGatewayForMalformedPokeApiPayloads() throws Exception
  {
    stubPokeApi.respondWith(200, """
      {
        "count": 1302,
        "next": null,
        "previous": null,
        "results": [
          {"name": "bulbasaur", "url": "https://pokeapi.co/api/v2/not-pokemon/1/"}
        ]
      }
      """, 0);

    mockMvc.perform(get("/api/pokemon").header("Authorization", bearerToken()))
      .andExpect(status().isBadGateway())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Der Pokémon-Katalog ist vorübergehend nicht verfügbar. Bitte versuche es später erneut.")));
  }

  @Test
  void getPageReturnsBadRequestWhenPagingParametersAreInvalid() throws Exception
  {
    mockMvc.perform(get("/api/pokemon?limit=abc").header("Authorization", bearerToken()))
      .andExpect(status().isBadRequest())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("The request parameters are invalid.")));
  }

  @Test
  void findByNameReturnsNotFoundForUnknownPokemonNames() throws Exception
  {
    stubPokeApi.respondWith(404, """
      {
        "detail": "Not found."
      }
      """, 0);

    mockMvc.perform(get("/api/pokemon/missingno").header("Authorization", bearerToken()))
      .andExpect(status().isNotFound())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Kein Pokémon mit dem Namen 'missingno' gefunden.")));
  }

  @Test
  void findByNameReturnsBadGatewayForMalformedPokeApiPayloads() throws Exception
  {
    stubPokeApi.respondWith(200, """
      {
        "species": "pikachu"
      }
      """, 0);

    mockMvc.perform(get("/api/pokemon/pikachu").header("Authorization", bearerToken()))
      .andExpect(status().isBadGateway())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Der Pokémon-Katalog ist vorübergehend nicht verfügbar. Bitte versuche es später erneut.")));
  }

  @Test
  void catalogEndpointsRequireAuthentication() throws Exception
  {
    mockMvc.perform(get("/api/pokemon"))
      .andExpect(status().isUnauthorized())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Authentifizierung erforderlich.")))
      .andExpect(jsonPath("$", hasKey("instance")));
  }

  private static final class StubPokeApi
  {

    private final AtomicReference<ResponseDefinition> response = new AtomicReference<>();

    void reset()
    {
      response.set(null);
    }

    void respondWith(int status, String body, long delayMillis)
    {
      response.set(new ResponseDefinition(status, body, delayMillis));
    }

    void handle(HttpExchange exchange) throws IOException
    {
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

  private static void ensureServerStarted() throws IOException
  {
    if (server != null)
    {
      return;
    }

    stubPokeApi = new StubPokeApi();
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/api/v2", stubPokeApi::handle);
    server.start();
  }

  private String bearerToken()
  {
    return "Bearer " + authService.generateToken("Chase");
  }
}
