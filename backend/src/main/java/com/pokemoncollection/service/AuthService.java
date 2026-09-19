package com.pokemoncollection.service;

import com.pokemoncollection.dto.TrainerSessionDto;
import com.pokemoncollection.entity.Trainer;
import com.pokemoncollection.exception.DuplicateTrainerNameException;
import com.pokemoncollection.exception.InvalidCredentialsException;
import com.pokemoncollection.repository.TrainerRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService
{

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

  private final TrainerRepository trainerRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthService(TrainerRepository trainerRepository, PasswordEncoder passwordEncoder)
  {
    this.trainerRepository = trainerRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public TrainerSessionDto register(String name, String password, HttpServletRequest request)
  {
    return register(name, password, request, null);
  }

  @Transactional
  public TrainerSessionDto register(String name, String password,
                                    HttpServletRequest request, HttpServletResponse response)
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

    authenticate(savedTrainer.getName(), request, response);
    LOGGER.info("Registered trainer {}", savedTrainer.getName());
    return new TrainerSessionDto(savedTrainer.getName());
  }

  @Transactional(readOnly = true)
  public TrainerSessionDto login(String name, String password,
                                 HttpServletRequest request, HttpServletResponse response)
  {
    String normalizedName = name.trim();
    Trainer trainer = trainerRepository.findByName(normalizedName).orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(password, trainer.getPasswordHash()))
    {
      throw new InvalidCredentialsException();
    }

    authenticate(trainer.getName(), request, response);
    LOGGER.info("Authenticated trainer {}", trainer.getName());
    return new TrainerSessionDto(trainer.getName());
  }

  public TrainerSessionDto currentSession(String username)
  {
    return new TrainerSessionDto(username);
  }

  private void authenticate(String trainerName, HttpServletRequest request, HttpServletResponse response)
  {
    var authentication = new UsernamePasswordAuthenticationToken(trainerName, null, List.of());
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);

    if (response != null)
    {
      new HttpSessionSecurityContextRepository().saveContext(context, request, response);
      return;
    }
    request.getSession(true)
      .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
  }
}
