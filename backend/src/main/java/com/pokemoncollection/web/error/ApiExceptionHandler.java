package com.pokemoncollection.web.error;

import com.pokemoncollection.exception.DuplicateTrainerNameException;
import com.pokemoncollection.exception.InvalidCredentialsException;
import com.pokemoncollection.exception.PokeApiUnavailableException;
import com.pokemoncollection.exception.PokemonAlreadyOwnedException;
import com.pokemoncollection.exception.PokemonNotFoundException;
import com.pokemoncollection.exception.TrainerNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class ApiExceptionHandler
{
  @ExceptionHandler(DuplicateTrainerNameException.class)
  public ResponseEntity<ProblemDetail> handleDuplicateTrainerName(DuplicateTrainerNameException exception,
                                                                  HttpServletRequest request)
  {
    return problem(HttpStatus.CONFLICT, exception.getMessage(), request);
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException exception,
                                                                HttpServletRequest request)
  {
    return problem(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
  }

  @ExceptionHandler(PokeApiUnavailableException.class)
  public ResponseEntity<ProblemDetail> handlePokeApiUnavailable(PokeApiUnavailableException exception,
                                                                HttpServletRequest request)
  {
    return problem(HttpStatus.BAD_GATEWAY, exception.getMessage(), request);
  }

  @ExceptionHandler(PokemonNotFoundException.class)
  public ResponseEntity<ProblemDetail> handlePokemonNotFound(PokemonNotFoundException exception,
                                                             HttpServletRequest request)
  {
    return problem(HttpStatus.NOT_FOUND, exception.getMessage(), request);
  }

  @ExceptionHandler(PokemonAlreadyOwnedException.class)
  public ResponseEntity<ProblemDetail> handlePokemonAlreadyOwned(PokemonAlreadyOwnedException exception,
                                                                 HttpServletRequest request)
  {
    return problem(HttpStatus.CONFLICT, exception.getMessage(), request);
  }

  @ExceptionHandler(TrainerNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleTrainerNotFound(TrainerNotFoundException exception,
                                                             HttpServletRequest request)
  {
    return problem(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidationFailure(MethodArgumentNotValidException exception,
                                                               HttpServletRequest request)
  {
    String detail = exception.getBindingResult().getFieldErrors().stream().findFirst().map(FieldError::getDefaultMessage).orElse("The request body is invalid.");
    return problem(HttpStatus.BAD_REQUEST, detail, request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleUnreadableBody(HttpMessageNotReadableException exception,
                                                            HttpServletRequest request)
  {
    return problem(HttpStatus.BAD_REQUEST, "The request body is invalid.", request);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ProblemDetail> handleArgumentTypeMismatch(MethodArgumentTypeMismatchException exception,
                                                                  HttpServletRequest request)
  {
    return problem(HttpStatus.BAD_REQUEST, "The request parameters are invalid.", request);
  }

  private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail, HttpServletRequest request)
  {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
    problemDetail.setTitle(status.getReasonPhrase());
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setType(URI.create("about:blank"));
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problemDetail);
  }
}
