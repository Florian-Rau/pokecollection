package com.pokemoncollection.service;

import com.pokemoncollection.dto.CollectionEntryDto;
import com.pokemoncollection.entity.CollectionEntry;
import com.pokemoncollection.entity.Trainer;
import com.pokemoncollection.exception.PokemonAlreadyOwnedException;
import com.pokemoncollection.exception.TrainerNotFoundException;
import com.pokemoncollection.repository.CollectionEntryRepository;
import com.pokemoncollection.repository.TrainerRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollectionService
{

  private final TrainerRepository trainerRepository;
  private final CollectionEntryRepository collectionEntryRepository;
  private final PokemonService pokemonService;

  public CollectionService(
    TrainerRepository trainerRepository,
    CollectionEntryRepository collectionEntryRepository,
    PokemonService pokemonService
  )
  {
    this.trainerRepository = trainerRepository;
    this.collectionEntryRepository = collectionEntryRepository;
    this.pokemonService = pokemonService;
  }

  @Transactional(readOnly = true)
  public List<CollectionEntryDto> getCollection(String trainerName)
  {
    Long trainerId = resolveTrainer(trainerName).getId();

    return collectionEntryRepository.findByTrainerId(trainerId).stream()
      .sorted(Comparator
                .comparing(CollectionEntry::getAddedAt)
                .thenComparing(CollectionEntry::getPokemonId))
      .map(entry -> new CollectionEntryDto(
        entry.getPokemonId(),
        entry.getPokemonName(),
        PokemonService.buildSpriteUrl(entry.getPokemonId()),
        entry.getAddedAt()
      ))
      .toList();
  }

  @Transactional
  public void addToCollection(String trainerName, Long pokemonId)
  {
    Trainer trainer = resolveTrainer(trainerName);
    String pokemonName = pokemonService.findById(pokemonId).name();

    try
    {
      collectionEntryRepository.save(new CollectionEntry(
        trainer,
        pokemonId,
        pokemonName,
        Instant.now()
      ));
    }
    catch (DataIntegrityViolationException exception)
    {
      throw new PokemonAlreadyOwnedException(pokemonId);
    }
  }

  private Trainer resolveTrainer(String trainerName)
  {
    return trainerRepository.findByName(trainerName).orElseThrow(TrainerNotFoundException::new);
  }
}
