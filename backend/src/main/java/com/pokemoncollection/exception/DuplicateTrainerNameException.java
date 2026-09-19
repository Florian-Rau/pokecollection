package com.pokemoncollection.exception;

public class DuplicateTrainerNameException extends RuntimeException
{
  public DuplicateTrainerNameException(String trainerName)
  {
    super("Trainername '%s' ist bereits registriert.".formatted(trainerName));
  }
}
