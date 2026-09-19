package com.pokemoncollection.exception;

public class InvalidCredentialsException extends RuntimeException
{
  public InvalidCredentialsException()
  {
    super("Ungültiger Trainer Name oder Passwort.");
  }
}
