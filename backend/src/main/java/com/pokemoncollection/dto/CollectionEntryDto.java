package com.pokemoncollection.dto;

import java.time.Instant;

public record CollectionEntryDto(Long pokemonId, String pokemonName, String spriteUrl, Instant addedAt)
{
}
