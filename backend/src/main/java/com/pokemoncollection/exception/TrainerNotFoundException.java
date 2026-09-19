package com.pokemoncollection.exception;

public class TrainerNotFoundException extends RuntimeException
{
  public TrainerNotFoundException()
  {
    super("Authentifizierung erforderlich.");
  }
}
