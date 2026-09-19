package com.pokemoncollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.pokemoncollection.dto.CollectionEntryDto;
import com.pokemoncollection.dto.PokemonSummaryDto;
import com.pokemoncollection.entity.CollectionEntry;
import com.pokemoncollection.entity.Trainer;
import com.pokemoncollection.exception.PokemonAlreadyOwnedException;
import com.pokemoncollection.exception.PokemonNotFoundException;
import com.pokemoncollection.repository.CollectionEntryRepository;
import com.pokemoncollection.repository.TrainerRepository;
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

@SpringBootTest
@ActiveProfiles("test")
@Import(CollectionServiceTest.TestConfig.class)
class CollectionServiceTest
{
  @Autowired
  private CollectionService collectionService;

  @Autowired
  private TrainerRepository trainerRepository;

  @Autowired
  private CollectionEntryRepository collectionEntryRepository;

  @Autowired
  private PokemonService pokemonService;

  @BeforeEach
  void setUp()
  {
    collectionEntryRepository.deleteAll();
    trainerRepository.deleteAll();
    reset(pokemonService);
  }

  @Test
  void getCollectionReturnsOnlyTheAuthenticatedTrainerEntries()
  {
    Trainer chase = trainerRepository.save(new Trainer("Chase", "hash-1"));
    Trainer misty = trainerRepository.save(new Trainer("Misty", "hash-2"));

    // Inserted out of chronological order to prove getCollection() sorts by addedAt itself,
    // rather than happening to preserve insertion order.
    collectionEntryRepository.save(new CollectionEntry(chase, 4L, "charmander", Instant.parse("2026-09-19T09:45:00Z")));
    collectionEntryRepository.save(new CollectionEntry(chase, 1L, "bulbasaur", Instant.parse("2026-09-18T08:15:00Z")));
    collectionEntryRepository.save(new CollectionEntry(misty, 7L, "squirtle", Instant.parse("2026-09-20T10:30:00Z")));

    assertThat(collectionService.getCollection("Chase"))
      .containsExactly(
        new CollectionEntryDto(1L, "bulbasaur",
                               "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
                               Instant.parse("2026-09-18T08:15:00Z")),
        new CollectionEntryDto(4L, "charmander",
                               "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/4.png",
                               Instant.parse("2026-09-19T09:45:00Z"))
      );
  }

  @Test
  void getCollectionReturnsAnEmptyListWhenTheTrainerHasNoEntries()
  {
    trainerRepository.save(new Trainer("Chase", "hash-1"));

    assertThat(collectionService.getCollection("Chase")).isEmpty();
  }

  @Test
  void getCollectionNeverLeaksAnotherTrainersEntries()
  {
    Trainer chase = trainerRepository.save(new Trainer("Chase", "hash-1"));
    Trainer brock = trainerRepository.save(new Trainer("Brock", "hash-2"));

    collectionEntryRepository.save(new CollectionEntry(chase, 25L, "pikachu", Instant.parse("2026-09-21T07:00:00Z")));
    collectionEntryRepository.save(new CollectionEntry(brock, 95L, "onix", Instant.parse("2026-09-21T07:05:00Z")));

    assertThat(collectionService.getCollection("Brock"))
      .containsExactly(new CollectionEntryDto(95L, "onix",
                                              "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/95.png",
                                              Instant.parse("2026-09-21T07:05:00Z")));
  }

  @Test
  void addToCollectionCreatesANewEntryForTheAuthenticatedTrainer()
  {
    trainerRepository.save(new Trainer("Chase", "hash-1"));
    when(pokemonService.findById(25L)).thenReturn(new PokemonSummaryDto(
      25L,
      "pikachu",
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png"
    ));

    Instant beforeAdd = Instant.now().minusSeconds(1);
    collectionService.addToCollection("Chase", 25L);
    Instant afterAdd = Instant.now();

    assertThat(collectionService.getCollection("Chase"))
      .singleElement()
      .satisfies(entry -> {
        assertThat(entry.pokemonId()).isEqualTo(25L);
        assertThat(entry.pokemonName()).isEqualTo("pikachu");
        assertThat(entry.addedAt()).isNotNull();
        assertThat(entry.addedAt()).isBetween(beforeAdd, afterAdd);
      });
  }

  @Test
  void addToCollectionRejectsDuplicateAddsForTheSameTrainer()
  {
    trainerRepository.save(new Trainer("Chase", "hash-1"));
    when(pokemonService.findById(25L)).thenReturn(new PokemonSummaryDto(
      25L,
      "pikachu",
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png"
    ));

    collectionService.addToCollection("Chase", 25L);

    assertThatThrownBy(() -> collectionService.addToCollection("Chase", 25L))
      .isInstanceOf(PokemonAlreadyOwnedException.class)
      .hasMessage("Pokémon mit der ID '25' befindet sich bereits in deiner Sammlung.");

    assertThat(collectionService.getCollection("Chase")).hasSize(1);
  }

  @Test
  void addToCollectionAllowsDifferentTrainersToOwnTheSamePokemonIndependently()
  {
    trainerRepository.save(new Trainer("Chase", "hash-1"));
    trainerRepository.save(new Trainer("Misty", "hash-2"));
    when(pokemonService.findById(25L)).thenReturn(new PokemonSummaryDto(
      25L,
      "pikachu",
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png"
    ));

    collectionService.addToCollection("Chase", 25L);
    collectionService.addToCollection("Misty", 25L);

    assertThat(collectionService.getCollection("Chase")).hasSize(1);
    assertThat(collectionService.getCollection("Misty")).hasSize(1);
    assertThat(collectionEntryRepository.count()).isEqualTo(2);
  }

  @Test
  void addToCollectionPropagatesUnknownPokemonIds()
  {
    trainerRepository.save(new Trainer("Chase", "hash-1"));
    when(pokemonService.findById(9999L)).thenThrow(new PokemonNotFoundException(9999L));

    assertThatThrownBy(() -> collectionService.addToCollection("Chase", 9999L))
      .isInstanceOf(PokemonNotFoundException.class)
      .hasMessage("Kein Pokémon mit der ID '9999' gefunden.");

    assertThat(collectionService.getCollection("Chase")).isEmpty();
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
}
