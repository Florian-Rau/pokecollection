package com.pokemoncollection.dto;

import jakarta.validation.constraints.NotNull;

public record AddCollectionEntryRequestDto(@NotNull(message = "Pokémon id is required.") Long pokemonId)
{
}
