package com.pokemoncollection.exception;

public class PokemonAlreadyOwnedException extends RuntimeException
{
  public PokemonAlreadyOwnedException(Long pokemonId)
  {
    super("Pokémon mit der ID '%d' befindet sich bereits in deiner Sammlung.".formatted(pokemonId));
  }
}
