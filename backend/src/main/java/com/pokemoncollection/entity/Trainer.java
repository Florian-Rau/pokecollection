package com.pokemoncollection.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "trainer")
public class Trainer
{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  protected Trainer()
  {
  }

  public Trainer(String name, String passwordHash)
  {
    this.name = name;
    this.passwordHash = passwordHash;
  }

  public Long getId()
  {
    return id;
  }

  public String getName()
  {
    return name;
  }

  public String getPasswordHash()
  {
    return passwordHash;
  }
}
