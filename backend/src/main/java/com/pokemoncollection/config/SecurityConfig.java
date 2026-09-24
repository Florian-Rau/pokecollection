package com.pokemoncollection.config;

import com.pokemoncollection.filter.JwtAuthFilter;
import com.pokemoncollection.web.error.RestAccessDeniedHandler;
import com.pokemoncollection.web.error.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig
{
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper,
                                          JwtAuthFilter jwtAuthFilter) throws Exception
  {
    http
      // Disable CSRF (not needed for stateless JWT)
      .csrf(csrf -> csrf.disable())

      // Configure endpoint authorization
      .authorizeHttpRequests(auth -> auth
        // Public endpoints
        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
        .requestMatchers("/", "/login", "/register", "/collection", "/browse",
                         "/index.html", "/favicon.ico", "/*.js", "/*.css", "/assets/**", "/media/**").permitAll()
        // All other endpoints require authentication
        .anyRequest().authenticated()
      )

      // Stateless session (required for JWT)
      .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

      // Add JWT filter before Spring Security's default filter
      .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
      .exceptionHandling(exceptions ->
                           exceptions.authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper))
                             .accessDeniedHandler(new RestAccessDeniedHandler(objectMapper)));

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder()
  {
    return new BCryptPasswordEncoder();
  }

}
