package com.pokemoncollection.repository;

import com.pokemoncollection.entity.Trainer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainerRepository extends JpaRepository<Trainer, Long>
{
  Optional<Trainer> findByName(String name);
}
