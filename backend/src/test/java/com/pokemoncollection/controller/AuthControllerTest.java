package com.pokemoncollection.controller;

import com.pokemoncollection.entity.Trainer;
import com.pokemoncollection.repository.CollectionEntryRepository;
import com.pokemoncollection.repository.TrainerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class AuthControllerTest
{
  private MockMvc mockMvc;

  @Autowired
  private TrainerRepository trainerRepository;

  @Autowired
  private CollectionEntryRepository collectionEntryRepository;

  @Autowired
  private WebApplicationContext applicationContext;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp()
  {
    mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).apply(springSecurity()).build();
    collectionEntryRepository.deleteAll();
    trainerRepository.deleteAll();
  }

  @Test
  void registerCreatesTrainerAndReturnsABearerToken(CapturedOutput output) throws Exception
  {
    var registration = mockMvc.perform(post("/api/auth/register")
                                        .contentType("application/json")
                                        .content("{\"name\":\"Chase\",\"password\":\"hunter22\"}"))
      .andExpect(status().isCreated())
      .andExpect(content().contentType("application/json"))
      .andExpect(jsonPath("$.username", equalTo("Chase")))
      .andExpect(jsonPath("$.token").isNotEmpty())
      .andReturn();

    String token = com.jayway.jsonpath.JsonPath.read(registration.getResponse().getContentAsString(), "$.token");
    assertThat(trainerRepository.count()).isEqualTo(1);

    mockMvc.perform(get("/api/auth/session").header("Authorization", "Bearer " + token))
      .andExpect(status().isNotFound());

    assertThat(output.getOut()).doesNotContain("hunter22").doesNotContain("$2a$");
  }

  @Test
  void loginReturnsABearerTokenAndDoesNotCreateASession() throws Exception
  {
    trainerRepository.save(new Trainer("Chase", passwordEncoder.encode("hunter22")));

    var login = mockMvc.perform(post("/api/auth/login")
                                 .contentType("application/json")
                                 .content("{\"name\":\"Chase\",\"password\":\"hunter22\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.username", equalTo("Chase")))
      .andExpect(jsonPath("$.token").isNotEmpty())
      .andReturn();

    assertThat(login.getRequest().getSession(false)).isNull();
  }

  @Test
  void loginRejectsInvalidCredentials() throws Exception
  {
    mockMvc.perform(post("/api/auth/login")
                      .contentType("application/json")
                      .content("{\"name\":\"Nobody\",\"password\":\"hunter22\"}"))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.detail", equalTo("Ungültiger Trainer Name oder Passwort.")));
  }

  @Test
  void registrationValidatesInputAndDuplicateNames() throws Exception
  {
    mockMvc.perform(post("/api/auth/register")
                      .contentType("application/json")
                      .content("{\"name\":\"\",\"password\":\"\"}"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.detail", is(oneOf(
        "Trainer name is required.",
        "Password is required.",
        "Password must be at least 6 characters."
      ))));

    mockMvc.perform(post("/api/auth/register")
                      .contentType("application/json")
                      .content("{\"name\":\"Chase\",\"password\":\"hunter22\"}"))
      .andExpect(status().isCreated());

    mockMvc.perform(post("/api/auth/register")
                      .contentType("application/json")
                      .content("{\"name\":\"Chase\",\"password\":\"hunter33\"}"))
      .andExpect(status().isConflict());
  }

  @Test
  void directNavigationToSpaRoutesForwardsToIndexHtml() throws Exception
  {
    mockMvc.perform(get("/login")).andExpect(forwardedUrl("/index.html"));
    mockMvc.perform(get("/register")).andExpect(forwardedUrl("/index.html"));
    mockMvc.perform(get("/collection")).andExpect(forwardedUrl("/index.html"));
    mockMvc.perform(get("/browse")).andExpect(forwardedUrl("/index.html"));
  }
}
