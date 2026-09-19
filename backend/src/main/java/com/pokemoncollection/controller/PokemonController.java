package com.pokemoncollection.controller;

import com.pokemoncollection.dto.PokemonPageDto;
import com.pokemoncollection.dto.PokemonSummaryDto;
import com.pokemoncollection.service.PokemonService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pokemon")
public class PokemonController
{
  private final PokemonService pokemonService;

  public PokemonController(PokemonService pokemonService)
  {
    this.pokemonService = pokemonService;
  }

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public PokemonPageDto getPage(
    @RequestParam(defaultValue = "20") Integer limit,
    @RequestParam(defaultValue = "0") Integer offset)
  {
    return pokemonService.getPage(limit, offset);
  }

  @GetMapping("/{name}")
  @ResponseStatus(HttpStatus.OK)
  public PokemonSummaryDto findByName(@PathVariable String name)
  {
    return pokemonService.findByName(name);
  }
}
