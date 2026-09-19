package com.pokemoncollection.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

public class RestAccessDeniedHandler implements AccessDeniedHandler
{
  private final ProblemResponseWriter problemResponseWriter;

  public RestAccessDeniedHandler(ObjectMapper objectMapper)
  {
    this.problemResponseWriter = new ProblemResponseWriter(objectMapper);
  }

  @Override
  public void handle(@NonNull HttpServletRequest request,
                     @NonNull HttpServletResponse response,
                     @NonNull AccessDeniedException accessDeniedException) throws IOException
  {
    problemResponseWriter.write(request, response, HttpStatus.FORBIDDEN, "Zugriff wurde verweigert.");
  }
}
