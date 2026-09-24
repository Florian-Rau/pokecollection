package com.pokemoncollection.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokemoncollection.dto.PokemonSummaryDto;
import com.pokemoncollection.entity.CollectionEntry;
import com.pokemoncollection.entity.Trainer;
import com.pokemoncollection.repository.CollectionEntryRepository;
import com.pokemoncollection.repository.TrainerRepository;
import com.pokemoncollection.exception.PokeApiUnavailableException;
import com.pokemoncollection.exception.PokemonNotFoundException;
import com.pokemoncollection.service.PokemonService;
import com.pokemoncollection.service.AuthService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Import(CollectionControllerTest.TestConfig.class)
class CollectionControllerTest
{
  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext applicationContext;

  @Autowired
  private TrainerRepository trainerRepository;

  @Autowired
  private CollectionEntryRepository collectionEntryRepository;

  @Autowired
  private PokemonService pokemonService;

  @Autowired
  private AuthService authService;

  @BeforeEach
  void setUp()
  {
    mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
      .apply(springSecurity())
      .build();
    collectionEntryRepository.deleteAll();
    trainerRepository.deleteAll();
    reset(pokemonService);
  }

  @Test
  void getCollectionReturnsTheAuthenticatedTrainerEntries() throws Exception
  {
    Trainer chase = trainerRepository.save(new Trainer("Chase", "hash-1"));
    Trainer misty = trainerRepository.save(new Trainer("Misty", "hash-2"));

    collectionEntryRepository.save(new CollectionEntry(chase, 1L, "bulbasaur", Instant.parse("2026-09-18T08:15:00Z")));
    collectionEntryRepository.save(new CollectionEntry(chase, 4L, "charmander", Instant.parse("2026-09-19T09:45:00Z")));
    collectionEntryRepository.save(new CollectionEntry(misty, 7L, "squirtle", Instant.parse("2026-09-20T10:30:00Z")));

    mockMvc.perform(get("/api/collection").header("Authorization", bearerToken("Chase")))
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/json"))
      .andExpect(jsonPath("$.length()", equalTo(2)))
      .andExpect(jsonPath("$[0].pokemonId", equalTo(1)))
      .andExpect(jsonPath("$[0].pokemonName", equalTo("bulbasaur")))
      .andExpect(jsonPath("$[0].addedAt", equalTo("2026-09-18T08:15:00Z")))
      .andExpect(jsonPath("$[1].pokemonId", equalTo(4)))
      .andExpect(jsonPath("$[1].pokemonName", equalTo("charmander")))
      .andExpect(jsonPath("$[1].addedAt", equalTo("2026-09-19T09:45:00Z")));
  }

  @Test
  void getCollectionReturnsAnEmptyArrayWhenTheTrainerHasNoEntries() throws Exception
  {
    trainerRepository.save(new Trainer("Chase", "hash-1"));

    mockMvc.perform(get("/api/collection").header("Authorization", bearerToken("Chase")))
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/json"))
      .andExpect(content().json("[]"));
  }

  @Test
  void getCollectionRequiresAuthentication() throws Exception
  {
    mockMvc.perform(get("/api/collection"))
      .andExpect(status().isUnauthorized())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Authentifizierung erforderlich.")))
      .andExpect(jsonPath("$", hasKey("instance")));
  }

  @Test
  void addToCollectionReturnsNoContentForASuccessfulAdd() throws Exception
  {
    trainerRepository.save(new Trainer("Chase", "hash-1"));
    when(pokemonService.findById(25L)).thenReturn(new PokemonSummaryDto(
      25L,
      "pikachu",
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png"
    ));

    mockMvc.perform(post("/api/collection")
                      .header("Authorization", bearerToken("Chase"))
                      .contentType("application/json")
                      .content("""
                                 {"pokemonId":25}
                                 """))
      .andExpect(status().isNoContent())
      .andExpect(content().string(""));

    mockMvc.perform(get("/api/collection").header("Authorization", bearerToken("Chase")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()", equalTo(1)))
      .andExpect(jsonPath("$[0].pokemonId", equalTo(25)))
      .andExpect(jsonPath("$[0].pokemonName", equalTo("pikachu")));
  }

  @Test
  void addToCollectionReturnsConflictWhenThePokemonIsAlreadyOwned() throws Exception
  {
    trainerRepository.save(new Trainer("Chase", "hash-1"));
    when(pokemonService.findById(25L)).thenReturn(new PokemonSummaryDto(
      25L,
      "pikachu",
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png"
    ));

    mockMvc.perform(post("/api/collection")
                      .header("Authorization", bearerToken("Chase"))
                      .contentType("application/json")
                      .content("""
                                 {"pokemonId":25}
                                 """))
      .andExpect(status().isNoContent());

    mockMvc.perform(post("/api/collection")
                      .header("Authorization", bearerToken("Chase"))
                      .contentType("application/json")
                      .content("""
                                 {"pokemonId":25}
                                 """))
      .andExpect(status().isConflict())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Pokémon mit der ID '25' befindet sich bereits in deiner Sammlung.")))
      .andExpect(jsonPath("$", hasKey("instance")));
  }

  @Test
  void addToCollectionReturnsNotFoundForUnknownPokemonIds() throws Exception
  {
    trainerRepository.save(new Trainer("Chase", "hash-1"));
    when(pokemonService.findById(9999L)).thenThrow(new PokemonNotFoundException(9999L));

    mockMvc.perform(post("/api/collection")
                      .header("Authorization", bearerToken("Chase"))
                      .contentType("application/json")
                      .content("""
                                 {"pokemonId":9999}
                                 """))
      .andExpect(status().isNotFound())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Kein Pokémon mit der ID '9999' gefunden.")));
  }

  @Test
  void addToCollectionReturnsBadRequestForMissingOrInvalidPokemonIds() throws Exception
  {
    trainerRepository.save(new Trainer("Chase", "hash-1"));

    mockMvc.perform(post("/api/collection")
                      .header("Authorization", bearerToken("Chase"))
                      .contentType("application/json")
                      .content("{}"))
      .andExpect(status().isBadRequest())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Pokémon id is required.")));

    mockMvc.perform(post("/api/collection")
                      .header("Authorization", bearerToken("Chase"))
                      .contentType("application/json")
                      .content("""
                                 {"pokemonId":"abc"}
                                 """))
      .andExpect(status().isBadRequest())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("The request body is invalid.")));
  }

  @Test
  void addToCollectionRequiresAuthentication() throws Exception
  {
    mockMvc.perform(post("/api/collection")
                      .contentType("application/json")
                      .content("""
                                 {"pokemonId":25}
                                 """))
      .andExpect(status().isUnauthorized())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Authentifizierung erforderlich.")))
      .andExpect(jsonPath("$", hasKey("instance")));
  }

  @Test
  void addToCollectionReturnsBadGatewayWhenPokemonLookupIsUnavailable() throws Exception
  {
    trainerRepository.save(new Trainer("Chase", "hash-1"));
    when(pokemonService.findById(25L)).thenThrow(new PokeApiUnavailableException());

    mockMvc.perform(post("/api/collection")
                      .header("Authorization", bearerToken("Chase"))
                      .contentType("application/json")
                      .content("""
                                 {"pokemonId":25}
                                 """))
      .andExpect(status().isBadGateway())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Der Pokémon-Katalog ist vorübergehend nicht verfügbar. Bitte versuche es später erneut.")));

    assertThat(collectionEntryRepository.count()).isZero();
  }

  @TestConfiguration
  static class TestConfig
  {
    @Bean
    @Primary
    PokemonService pokemonService()
    {
      return mock(PokemonService.class);
    }

  }

  private String bearerToken(String username)
  {
    return "Bearer " + authService.generateToken(username);
  }
}
