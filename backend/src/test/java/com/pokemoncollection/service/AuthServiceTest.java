package com.pokemoncollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokemoncollection.exception.DuplicateTrainerNameException;
import com.pokemoncollection.exception.InvalidCredentialsException;
import com.pokemoncollection.repository.CollectionEntryRepository;
import com.pokemoncollection.repository.TrainerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest
{
  @Autowired
  private AuthService authService;

  @Autowired
  private TrainerRepository trainerRepository;

  @Autowired
  private CollectionEntryRepository collectionEntryRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp()
  {
    collectionEntryRepository.deleteAll();
    trainerRepository.deleteAll();
  }

  @Test
  void registerHashesPasswordAndReturnsAToken()
  {
      var loginResponse = authService.register("Chase", "hunter22");

    var trainer = trainerRepository.findByName("Chase").orElseThrow();
    assertThat(loginResponse.username()).isEqualTo("Chase");
    assertThat(loginResponse.token()).isNotBlank();
    assertThat(authService.extractUsername(loginResponse.token())).isEqualTo("Chase");
    assertThat(trainer.getPasswordHash()).isNotEqualTo("hunter22");
    assertThat(passwordEncoder.matches("hunter22", trainer.getPasswordHash())).isTrue();
  }

  @Test
  void loginReturnsATokenForValidCredentials()
  {
    trainerRepository.save(new com.pokemoncollection.entity.Trainer("Chase", passwordEncoder.encode("hunter22")));

    var loginResponse = authService.login(" Chase ", "hunter22");

    assertThat(loginResponse.username()).isEqualTo("Chase");
    assertThat(authService.extractUsername(loginResponse.token())).isEqualTo("Chase");
  }

  @Test
  void loginRejectsInvalidCredentials()
  {
    assertThatThrownBy(() -> authService.login("Nobody", "hunter22"))
      .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void registerRejectsDuplicateTrainerNames()
  {
    authService.register("Chase", "hunter22");

    assertThatThrownBy(() -> authService.register("Chase", "hunter33"))
      .isInstanceOf(DuplicateTrainerNameException.class)
      .hasMessage("Trainername 'Chase' ist bereits registriert.");
    assertThat(trainerRepository.count()).isEqualTo(1);
  }
}
