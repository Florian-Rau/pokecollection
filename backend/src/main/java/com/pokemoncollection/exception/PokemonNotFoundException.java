package com.pokemoncollection.exception;

public class PokemonNotFoundException extends RuntimeException
{
  public PokemonNotFoundException(String name)
  {
    super("Kein Pokémon mit dem Namen '" + name + "' gefunden.");
  }
  public PokemonNotFoundException(Long pokemonId)
  {
    super("Kein Pokémon mit der ID '" + pokemonId + "' gefunden.");
  }
}
