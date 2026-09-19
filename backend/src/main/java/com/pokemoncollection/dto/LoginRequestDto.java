package com.pokemoncollection.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(@NotBlank(message = "Trainer name is required.") String name,

                              @NotBlank(message = "Password is required.") String password)
{
}
