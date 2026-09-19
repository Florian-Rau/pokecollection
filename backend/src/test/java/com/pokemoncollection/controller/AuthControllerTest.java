package com.pokemoncollection.controller;

import com.pokemoncollection.entity.Trainer;
import com.pokemoncollection.repository.CollectionEntryRepository;
import com.pokemoncollection.repository.TrainerRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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

  @Value("${local.server.port}")
  private int port;

  private final HttpClient httpClient = HttpClient.newBuilder()
    .build();

  @BeforeEach
  void setUp()
  {
    mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
      .apply(springSecurity())
      .build();
    collectionEntryRepository.deleteAll();
    trainerRepository.deleteAll();
  }

  private void createTrainer(String name, String password)
  {
    trainerRepository.save(new Trainer(name, passwordEncoder.encode(password)));
  }

  @Test
  void registerCreatesTrainerAndSupportsSubsequentAuthenticatedRequests(CapturedOutput output) throws Exception
  {
    MvcResult registration = mockMvc.perform(post("/api/auth/register")
                                               .with(csrf())
                                               .contentType("application/json")
                                               .content("""
                                                          {"name":"Chase","password":"hunter22"}
                                                          """))
      .andExpect(status().isCreated())
      .andExpect(content().contentType("application/json"))
      .andExpect(jsonPath("$.username", equalTo("Chase")))
      .andReturn();

    assertThat(trainerRepository.count()).isEqualTo(1);
    assertThat(registration.getRequest().getSession(false)).isNotNull();

    mockMvc.perform(get("/api/auth/session")
                      .session((MockHttpSession)Objects.requireNonNull(registration.getRequest().getSession(false))))
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/json"))
      .andExpect(jsonPath("$.username", equalTo("Chase")));

    assertThat(output.getOut()).doesNotContain("hunter22");
    assertThat(output.getOut()).doesNotContain("$2a$")
      .doesNotContain("$2b$")
      .doesNotContain("$2y$");
  }

  @Test
  void registerRejectsDuplicateTrainerNames() throws Exception
  {
    mockMvc.perform(post("/api/auth/register")
                      .with(csrf())
                      .contentType("application/json")
                      .content("""
                                 {"name":"Chase","password":"hunter22"}
                                 """))
      .andExpect(status().isCreated());

    mockMvc.perform(post("/api/auth/register")
                      .with(csrf())
                      .contentType("application/json")
                      .content("""
                                 {"name":"Chase","password":"hunter33"}
                                 """))
      .andExpect(status().isConflict())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Trainername 'Chase' ist bereits registriert.")));

    assertThat(trainerRepository.count()).isEqualTo(1);
  }

  @Test
  void registerRejectsBlankInput() throws Exception
  {
    mockMvc.perform(post("/api/auth/register")
                      .with(csrf())
                      .contentType("application/json")
                      .content("""
                                 {"name":"","password":""}
                                 """))
      .andExpect(status().isBadRequest())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", is(oneOf(
        "Trainer name is required.",
        "Password is required.",
        "Password must be at least 6 characters."
      ))));

    assertThat(trainerRepository.count()).isZero();
  }

  @Test
  void registerRejectsShortPasswords() throws Exception
  {
    mockMvc.perform(post("/api/auth/register")
                      .with(csrf())
                      .contentType("application/json")
                      .content("""
                                 {"name":"Chase","password":"abc"}
                                 """))
      .andExpect(status().isBadRequest())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Password must be at least 6 characters.")));

    assertThat(trainerRepository.count()).isZero();
  }

  @Test
  void registerRejectsOverlongNamesAsValidationFailuresNotDuplicates() throws Exception
  {
    String overlongName = "A".repeat(300);

    mockMvc.perform(post("/api/auth/register")
                      .with(csrf())
                      .contentType("application/json")
                      .content("{\"name\":\"" + overlongName + "\",\"password\":\"hunter22\"}"))
      .andExpect(status().isBadRequest())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Trainer name must be at most 255 characters.")));

    assertThat(trainerRepository.count()).isZero();
  }

  @Test
  void loginAuthenticatesKnownTrainerAndSupportsSubsequentAuthenticatedRequests(CapturedOutput output) throws Exception
  {
    createTrainer("Chase", "hunter22");

    MvcResult login = mockMvc.perform(post("/api/auth/login")
                                        .with(csrf())
                                        .contentType("application/json")
                                        .content("""
                                                   {"name":"Chase","password":"hunter22"}
                                                   """))
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/json"))
      .andExpect(jsonPath("$.username", equalTo("Chase")))
      .andReturn();

    assertThat(login.getRequest().getSession(false)).isNotNull();

    mockMvc.perform(get("/api/auth/session").session((MockHttpSession)
                                                       Objects.requireNonNull(login.getRequest().getSession(false))))
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/json"))
      .andExpect(jsonPath("$.username", equalTo("Chase")));

    assertThat(output.getOut()).doesNotContain("hunter22");
    assertThat(output.getOut()).doesNotContain("$2a$")
      .doesNotContain("$2b$")
      .doesNotContain("$2y$");
  }

  @Test
  void loginRejectsUnknownTrainerNamesWithAGenericUnauthorizedResponse() throws Exception
  {
    MvcResult login = mockMvc.perform(post("/api/auth/login")
                                        .with(csrf())
                                        .contentType("application/json")
                                        .content("""
                                                   {"name":"Nobody","password":"hunter22"}
                                                   """))
      .andExpect(status().isUnauthorized())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Ungültiger Trainer Name oder Passwort.")))
      .andReturn();

    assertThat(login.getRequest().getSession(false)).isNull();
  }

  @Test
  void loginRejectsWrongPasswordsWithTheSameGenericUnauthorizedResponse() throws Exception
  {
    createTrainer("Chase", "hunter22");

    MvcResult login = mockMvc.perform(post("/api/auth/login")
                                        .with(csrf())
                                        .contentType("application/json")
                                        .content("""
                                                   {"name":"Chase","password":"bad"}
                                                   """))
      .andExpect(status().isUnauthorized())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Ungültiger Trainer Name oder Passwort.")))
      .andReturn();

    assertThat(login.getRequest().getSession(false)).isNull();
  }

  @Test
  void loginRejectsBlankOrMissingInput() throws Exception
  {
    mockMvc.perform(post("/api/auth/login")
                      .with(csrf())
                      .contentType("application/json")
                      .content("""
                                 {"name":"","password":""}
                                 """))
      .andExpect(status().isBadRequest())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", is(oneOf(
        "Trainer name is required.",
        "Password is required."
      ))));

    mockMvc.perform(post("/api/auth/login")
                      .with(csrf())
                      .contentType("application/json")
                      .content("""
                                 {"name":"Chase"}
                                 """))
      .andExpect(status().isBadRequest())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Password is required.")));
  }

  @Test
  void logoutInvalidatesTheSessionAndRejectsFurtherRequestsWithTheOldSession() throws Exception
  {
    createTrainer("Chase", "hunter23");

    MvcResult login = mockMvc.perform(post("/api/auth/login")
                                        .with(csrf())
                                        .contentType("application/json")
                                        .content("""
                                                   {"name":"Chase","password":"hunter23"}
                                                   """))
      .andExpect(status().isOk())
      .andReturn();

    MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
    assertThat(session).isNotNull();

    mockMvc.perform(post("/api/auth/logout")
                      .with(csrf())
                      .session(session))
      .andExpect(status().isFound())
      .andExpect(content().string(""));

    mockMvc.perform(get("/api/auth/session").session(session))
      .andExpect(status().isUnauthorized())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Authentifizierung erforderlich.")))
      .andExpect(jsonPath("$", hasKey("instance")));
  }

  @Test
  void logoutRequiresAnAuthenticatedSession() throws Exception
  {
    mockMvc.perform(post("/api/auth/logout").with(csrf()))
      .andExpect(status().isFound());
  }

  @Test
  void logoutWithoutTheCsrfTokenIsRejectedAndLeavesTheSessionUsable() throws Exception
  {
    createTrainer("Red", "hunter22");

    MvcResult login = mockMvc.perform(post("/api/auth/login")
                                        .with(csrf())
                                        .contentType("application/json")
                                        .content("""
                                                   {"name":"Red","password":"hunter22"}
                                                   """))
      .andExpect(status().isOk())
      .andReturn();

    MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
    assertThat(session).isNotNull();

    mockMvc.perform(post("/api/auth/logout").session(session))
      .andExpect(status().isForbidden())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Zugriff wurde verweigert.")))
      .andExpect(jsonPath("$", hasKey("instance")));

    mockMvc.perform(get("/api/auth/session").session(session))
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/json"))
      .andExpect(jsonPath("$.username", equalTo("Red")));
  }

  @Test
  void registrationWithoutTheCsrfCookieIsRejected() throws Exception
  {
    URI baseUri = URI.create("http://localhost:" + port);

    HttpResponse<String> registration = httpClient.send(
      HttpRequest.newBuilder(baseUri.resolve("/api/auth/register"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"Chase\",\"password\":\"hunter22\"}"))
        .build(),
      HttpResponse.BodyHandlers.ofString()
    );

    assertThat(registration.statusCode()).isEqualTo(403);
    assertThat(trainerRepository.count()).isZero();
  }

  @Test
  void loginWithoutTheCsrfCookieIsRejected() throws Exception
  {
    createTrainer("Chase", "hunter22");
    URI baseUri = URI.create("http://localhost:" + port);

    HttpResponse<String> login = httpClient.send(
      HttpRequest.newBuilder(baseUri.resolve("/api/auth/login"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"Chase\",\"password\":\"hunter22\"}"))
        .build(),
      HttpResponse.BodyHandlers.ofString()
    );

    assertThat(login.statusCode()).isEqualTo(403);
  }

  @Test
  void directNavigationToSpaRoutesForwardsToIndexHtml() throws Exception
  {
    mockMvc.perform(get("/login"))
      .andExpect(forwardedUrl("/index.html"));
    mockMvc.perform(get("/register"))
      .andExpect(forwardedUrl("/index.html"));
    mockMvc.perform(get("/collection"))
      .andExpect(forwardedUrl("/index.html"));
    mockMvc.perform(get("/browse"))
      .andExpect(forwardedUrl("/index.html"));
  }

  @Test
  void sessionEndpointRequiresAuthentication() throws Exception
  {
    mockMvc.perform(get("/api/auth/session"))
      .andExpect(status().isUnauthorized())
      .andExpect(content().contentType("application/problem+json"))
      .andExpect(jsonPath("$.detail", equalTo("Authentifizierung erforderlich.")))
      .andExpect(jsonPath("$", hasKey("instance")));
  }
}
