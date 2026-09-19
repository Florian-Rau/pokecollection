package com.pokemoncollection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
  @NotBlank(message = "Trainer name is required.") @Size(max = 255, message = "Trainer name must be at most 255 characters.") String name,

  @NotBlank(message = "Password is required.") @Size(min = 6, message = "Password must be at least 6 characters.") String password)
{
}
