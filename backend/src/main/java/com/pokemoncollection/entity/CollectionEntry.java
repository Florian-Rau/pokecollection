package com.pokemoncollection.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "collection_entry")
public class CollectionEntry
{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "trainer_id", nullable = false)
  private Trainer trainer;

  @Column(name = "pokemon_id", nullable = false)
  private Long pokemonId;

  @Column(name = "pokemon_name", nullable = false)
  private String pokemonName;

  @Column(name = "added_at", nullable = false)
  private Instant addedAt;

  protected CollectionEntry()
  {
  }

  public CollectionEntry(Trainer trainer, Long pokemonId, String pokemonName, Instant addedAt)
  {
    this.trainer = trainer;
    this.pokemonId = pokemonId;
    this.pokemonName = pokemonName;
    this.addedAt = addedAt;
  }

  public Long getId()
  {
    return id;
  }

  public Trainer getTrainer()
  {
    return trainer;
  }

  public Long getPokemonId()
  {
    return pokemonId;
  }

  public String getPokemonName()
  {
    return pokemonName;
  }

  public Instant getAddedAt()
  {
    return addedAt;
  }
}
