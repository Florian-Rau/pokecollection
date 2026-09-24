package com.pokemoncollection.controller;

import com.pokemoncollection.dto.AuthenticationResponseDto;
import com.pokemoncollection.dto.LoginRequestDto;
import com.pokemoncollection.dto.RegisterRequestDto;
import com.pokemoncollection.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
  public AuthenticationResponseDto login(@Valid @RequestBody LoginRequestDto request)
  {
    return authService.login(request.name(), request.password());
  }

  @PostMapping("/register")
  public ResponseEntity<AuthenticationResponseDto> register(@Valid @RequestBody RegisterRequestDto request)
  {
    AuthenticationResponseDto response = authService.register(request.name(), request.password());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(Authentication authentication)
  {
    authService.logout(authentication.getName());
  }
}
