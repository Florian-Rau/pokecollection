package com.pokemoncollection.service;

import com.pokemoncollection.dto.PokemonPageDto;
import com.pokemoncollection.dto.PokemonSummaryDto;
import com.pokemoncollection.exception.PokeApiUnavailableException;
import com.pokemoncollection.exception.PokemonNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Service
public class PokemonService
{

  private static final Pattern POKEMON_ID_PATTERN = Pattern.compile(".*/pokemon/(\\d+)/?$");
  private static final String SPRITE_URL_TEMPLATE =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/%d.png";

  private final RestClient pokeApiRestClient;

  public PokemonService(@Qualifier("pokeApiRestClient") RestClient pokeApiRestClient)
  {
    this.pokeApiRestClient = pokeApiRestClient;
  }

  public PokemonPageDto getPage(Integer limit, Integer offset)
  {
    try
    {
      PokeApiPageResponse response = pokeApiRestClient.get().uri(uriBuilder -> uriBuilder.path("/pokemon").queryParam("limit", limit).queryParam("offset", offset).build()).retrieve().body(PokeApiPageResponse.class);

      if (response == null || response.count() == null || response.results() == null)
      {
        throw new PokeApiUnavailableException();
      }

      return new PokemonPageDto(response.results().stream().map(this::toSummary).toList(), response.count(), response.next(), response.previous());
    }
    catch (PokeApiUnavailableException exception)
    {
      throw exception;
    }
    catch (RuntimeException exception)
    {
      throw new PokeApiUnavailableException(exception);
    }
  }

  public PokemonSummaryDto findByName(String name)
  {
    String normalizedName = name.trim().toLowerCase(Locale.ROOT);
    return findPokemon(normalizedName, () -> new PokemonNotFoundException(normalizedName));
  }

  public PokemonSummaryDto findById(Long pokemonId)
  {
    return findPokemon(String.valueOf(pokemonId), () -> new PokemonNotFoundException(pokemonId));
  }

  private PokemonSummaryDto findPokemon(String identifier,
                                        Supplier<PokemonNotFoundException> notFoundExceptionSupplier)
  {
    try
    {
      PokeApiPokemonResponse response = pokeApiRestClient
        .get().uri("/pokemon/{identifier}", identifier).retrieve().body(PokeApiPokemonResponse.class);

      if (response == null || response.id() == null || response.name() == null)
      {
        throw new PokeApiUnavailableException();
      }

      return new PokemonSummaryDto(response.id(), response.name(), buildSpriteUrl(response.id()));
    }
    catch (HttpClientErrorException.NotFound exception)
    {
      throw notFoundExceptionSupplier.get();
    }
    catch (PokeApiUnavailableException exception)
    {
      throw exception;
    }
    catch (RuntimeException exception)
    {
      throw new PokeApiUnavailableException(exception);
    }
  }

  private PokemonSummaryDto toSummary(PokeApiListItem item)
  {
    if (item == null || item.name() == null || item.url() == null)
    {
      throw new PokeApiUnavailableException();
    }

    long pokemonId = extractPokemonId(item.url());
    return new PokemonSummaryDto(pokemonId, item.name(), buildSpriteUrl(pokemonId));
  }

  private long extractPokemonId(String url)
  {
    Matcher matcher = POKEMON_ID_PATTERN.matcher(url);
    if (!matcher.matches())
    {
      throw new PokeApiUnavailableException();
    }

    return Long.parseLong(matcher.group(1));
  }

  public static String buildSpriteUrl(long pokemonId)
  {
    return SPRITE_URL_TEMPLATE.formatted(pokemonId);
  }

  private record PokeApiPageResponse(Long count, String next, String previous, List<PokeApiListItem> results)
  {
  }

  private record PokeApiListItem(String name, String url)
  {
  }

  private record PokeApiPokemonResponse(Long id, String name)
  {
  }
}
