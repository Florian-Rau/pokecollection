package com.pokemoncollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokemoncollection.dto.TrainerSessionDto;
import com.pokemoncollection.exception.DuplicateTrainerNameException;
import com.pokemoncollection.repository.CollectionEntryRepository;
import com.pokemoncollection.repository.TrainerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
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
  void registerHashesPasswordAndStoresAuthenticatedSession()
  {
    MockHttpServletRequest request = new MockHttpServletRequest();

    TrainerSessionDto session = authService.register("Chase", "hunter22", request);

    var trainer = trainerRepository.findByName("Chase").orElseThrow();
    assertThat(session.username()).isEqualTo("Chase");
    assertThat(trainer.getPasswordHash()).isNotEqualTo("hunter22");
    assertThat(passwordEncoder.matches("hunter22", trainer.getPasswordHash())).isTrue();
    assertThat(request.getSession(false)).isNotNull();
    assertThat(request.getSession(false).getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
      .isNotNull();
  }

  @Test
  void registerRejectsDuplicateTrainerNames()
  {
    MockHttpServletRequest request = new MockHttpServletRequest();
    authService.register("Chase", "hunter22", request);

    assertThatThrownBy(() -> authService.register("Chase", "hunter33", new MockHttpServletRequest()))
      .isInstanceOf(DuplicateTrainerNameException.class)
      .hasMessage("Trainername 'Chase' ist bereits registriert.");
    assertThat(trainerRepository.count()).isEqualTo(1);
  }
}
