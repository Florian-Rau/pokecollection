package com.pokemoncollection.controller;

import com.pokemoncollection.dto.AddCollectionEntryRequestDto;
import com.pokemoncollection.dto.CollectionEntryDto;
import com.pokemoncollection.service.CollectionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/collection")
public class CollectionController
{
  private final CollectionService collectionService;

  public CollectionController(CollectionService collectionService)
  {
    this.collectionService = collectionService;
  }

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public List<CollectionEntryDto> getCollection(Authentication authentication)
  {
    return collectionService.getCollection(authentication.getName());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void addToCollection(@Valid @RequestBody AddCollectionEntryRequestDto request, Authentication authentication)
  {
    collectionService.addToCollection(authentication.getName(), request.pokemonId());
  }
}
