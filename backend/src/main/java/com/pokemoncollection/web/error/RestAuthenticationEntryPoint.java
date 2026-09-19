package com.pokemoncollection.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint
{
  private final ProblemResponseWriter problemResponseWriter;

  public RestAuthenticationEntryPoint(ObjectMapper objectMapper)
  {
    this.problemResponseWriter = new ProblemResponseWriter(objectMapper);
  }

  @Override
  public void commence(@NonNull HttpServletRequest request,
                       @NonNull HttpServletResponse response,
                       @NonNull AuthenticationException authException) throws IOException
  {
    problemResponseWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Authentifizierung erforderlich.");
  }
}
