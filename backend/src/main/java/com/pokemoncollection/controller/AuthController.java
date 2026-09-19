package com.pokemoncollection.controller;

import com.pokemoncollection.dto.LoginRequestDto;
import com.pokemoncollection.dto.RegisterRequestDto;
import com.pokemoncollection.dto.TrainerSessionDto;
import com.pokemoncollection.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController
{

  private final AuthService authService;

  public AuthController(AuthService authService)
  {
    this.authService = authService;
  }

  @PostMapping("/login")
  @ResponseStatus(HttpStatus.OK)
  public TrainerSessionDto login(@Valid @RequestBody LoginRequestDto request,
                                 HttpServletRequest httpServletRequest,
                                 HttpServletResponse httpServletResponse)
  {
    return authService.login(request.name(), request.password(), httpServletRequest, httpServletResponse);
  }

  @PostMapping("/register")
  public ResponseEntity<TrainerSessionDto> register(@Valid @RequestBody RegisterRequestDto request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
  {
    TrainerSessionDto session = authService.register(request.name(), request.password(), httpServletRequest, httpServletResponse);
    return ResponseEntity.status(HttpStatus.CREATED).body(session);
  }

  @GetMapping("/session")
  @ResponseStatus(HttpStatus.OK)
  public TrainerSessionDto session(Authentication authentication)
  {
    return authService.currentSession(authentication.getName());
  }
}
