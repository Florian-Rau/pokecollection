package com.pokemoncollection.dto;

import java.util.List;

public record PokemonPageDto(List<PokemonSummaryDto> results, Long count, String next, String previous)
{
}
