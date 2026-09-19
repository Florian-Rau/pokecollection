package com.pokemoncollection.repository;

import com.pokemoncollection.entity.CollectionEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionEntryRepository extends JpaRepository<CollectionEntry, Long>
{
  List<CollectionEntry> findByTrainerId(Long trainerId);
}
