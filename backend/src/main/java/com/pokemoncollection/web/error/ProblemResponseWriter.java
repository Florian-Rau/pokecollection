package com.pokemoncollection.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

final class ProblemResponseWriter
{
  private final ObjectMapper objectMapper;

  ProblemResponseWriter(ObjectMapper objectMapper)
  {
    this.objectMapper = objectMapper;
  }

  void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String detail)
    throws IOException
  {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
    problemDetail.setTitle(status.getReasonPhrase());
    problemDetail.setType(URI.create("about:blank"));
    problemDetail.setInstance(URI.create(request.getRequestURI()));

    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problemDetail);
  }
}
