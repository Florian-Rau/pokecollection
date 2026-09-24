package com.pokemoncollection.filter;

import com.pokemoncollection.service.AuthService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter
{
  private final UserDetailsService userDetailsService;
  private final AuthService authService;

  @Autowired
  public JwtAuthFilter(UserDetailsService userDetailsService, AuthService jwtService)
  {
    this.userDetailsService = userDetailsService;
    this.authService = jwtService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException
  {
    String authHeader = request.getHeader("Authorization");
    String token = null;
    String username = null;

    if (authHeader != null && authHeader.startsWith("Bearer "))
    {
      token = authHeader.substring(7);
      try
      {
        username = authService.extractUsername(token);
      }
      catch (JwtException | IllegalArgumentException exception)
      {
        filterChain.doFilter(request, response);
        return;
      }
    }

    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null)
    {
      UserDetails userDetails = userDetailsService.loadUserByUsername(username);
      if (authService.validateToken(token, userDetails))
      {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
                                                                                                null,
                                                                                                userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    }
    filterChain.doFilter(request, response);
  }
}
