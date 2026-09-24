package com.pokemoncollection.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "trainer")
public class Trainer implements UserDetails
{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "token_version", nullable = false)
  private long tokenVersion;

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

  public String getPasswordHash()
  {
    return passwordHash;
  }

  public long getTokenVersion()
  {
    return tokenVersion;
  }

  public void revokeTokens()
  {
    tokenVersion++;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities()
  {
    return List.of();
  }

  @Override
  public @Nullable String getPassword()
  {
    return passwordHash;
  }

  @Override
  public String getUsername()
  {
    return name;
  }
}
