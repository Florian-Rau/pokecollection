package com.pokemoncollection.service;

import com.pokemoncollection.dto.AuthenticationResponseDto;
import com.pokemoncollection.entity.Trainer;
import com.pokemoncollection.exception.DuplicateTrainerNameException;
import com.pokemoncollection.repository.TrainerRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements UserDetailsService
{
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

  private final TrainerRepository trainerRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${security.jwt.secret-key}")
  private String secretKey;

  @Value("${security.jwt.expiration-time}")
  private long jwtExpiration;

  public AuthService(TrainerRepository trainerRepository, PasswordEncoder passwordEncoder)
  {
    this.trainerRepository = trainerRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public AuthenticationResponseDto register(String name, String password)
  {
    String normalizedName = name.trim();

    trainerRepository.findByName(normalizedName).ifPresent(existingTrainer -> {
      throw new DuplicateTrainerNameException(normalizedName);
    });

    Trainer trainer = new Trainer(normalizedName, passwordEncoder.encode(password));

    Trainer savedTrainer;
    try
    {
      savedTrainer = trainerRepository.save(trainer);
    }
    catch (DataIntegrityViolationException exception)
    {
      throw new DuplicateTrainerNameException(normalizedName);
    }

    LOGGER.info("Registered trainer {}", savedTrainer.getUsername());
    return new AuthenticationResponseDto(savedTrainer.getUsername(), generateToken(savedTrainer));
  }

  @Transactional(readOnly = true)
  public AuthenticationResponseDto login(String name, String password)
  {
    String normalizedName = name.trim();
    Trainer trainer = trainerRepository.findByName(normalizedName)
      .orElseThrow(com.pokemoncollection.exception.InvalidCredentialsException::new);

    if (!passwordEncoder.matches(password, trainer.getPasswordHash()))
    {
      throw new com.pokemoncollection.exception.InvalidCredentialsException();
    }

    LOGGER.info("Authenticated trainer {}", trainer.getUsername());
    return new AuthenticationResponseDto(trainer.getUsername(), generateToken(trainer));
  }

  @Transactional
  public void logout(String username)
  {
    Trainer trainer = trainerRepository.findByName(username)
      .orElseThrow(() -> new UsernameNotFoundException("Trainer not found: " + username));
    trainer.revokeTokens();
    LOGGER.info("Logged out trainer {}", trainer.getUsername());
  }

  public String generateToken(String userName)
  {
    Trainer trainer = trainerRepository.findByName(userName)
      .orElseThrow(() -> new UsernameNotFoundException("Trainer not found: " + userName));
    return generateToken(trainer);
  }

  private String generateToken(Trainer trainer)
  {
    return Jwts.builder()
      .subject(trainer.getUsername())
      .claim("tokenVersion", trainer.getTokenVersion())
      .issuedAt(new Date())
      .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
      .signWith(getSignKey())
      .compact();
  }

  private SecretKey getSignKey()
  {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public String extractUsername(String token)
  {
    return extractClaim(token, Claims::getSubject);
  }

  public Date extractExpiration(String token)
  {
    return extractClaim(token, Claims::getExpiration);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver)
  {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token)
  {
    return Jwts.parser()
      .verifyWith(getSignKey())
      .build()
      .parseSignedClaims(token)
      .getPayload();
  }

  private Boolean isTokenExpired(String token)
  {
    return extractExpiration(token).before(new Date());
  }

  public Boolean validateToken(String token, UserDetails userDetails)
  {
    final String username = extractUsername(token);
    if (!(userDetails instanceof Trainer trainer))
    {
      return false;
    }

    Number tokenVersion = extractClaim(token, claims -> claims.get("tokenVersion", Number.class));
    return username.equals(trainer.getUsername())
      && tokenVersion != null
      && tokenVersion.longValue() == trainer.getTokenVersion()
      && !isTokenExpired(token);
  }

  @Override
  public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException
  {
    return trainerRepository.findByName(username).orElseThrow(() -> new UsernameNotFoundException("Trainer not found: " + username));
  }
}
