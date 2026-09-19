package com.pokemoncollection.exception;

public class PokeApiUnavailableException extends RuntimeException
{
  public PokeApiUnavailableException()
  {
    super("Der Pokémon-Katalog ist vorübergehend nicht verfügbar. Bitte versuche es später erneut.");
  }

  public PokeApiUnavailableException(Throwable cause)
  {
    super("Der Pokémon-Katalog ist vorübergehend nicht verfügbar. Bitte versuche es später erneut.", cause);
  }
}
