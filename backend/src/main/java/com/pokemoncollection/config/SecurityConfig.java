package com.pokemoncollection.config;

import com.pokemoncollection.web.error.RestAccessDeniedHandler;
import com.pokemoncollection.web.error.RestAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig
{

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper, CookieCsrfTokenRepository csrfTokenRepository) throws Exception
  {
    return http.csrf(csrf -> csrf
      .csrfTokenRepository(csrfTokenRepository)
      .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
      .authorizeHttpRequests(authorize -> authorize.requestMatchers(HttpMethod.POST, "/api/auth/login")
        .permitAll().requestMatchers(HttpMethod.POST, "/api/auth/register")
        .permitAll().requestMatchers("/", "/index.html", "/favicon.ico", "/*.js", "/*.css", "/assets/**", "/media/**").permitAll().requestMatchers("/api/**")
        .authenticated().anyRequest()
        .permitAll())
      .securityContext(Customizer.withDefaults())
      .sessionManagement(Customizer.withDefaults())
      .exceptionHandling(exceptions ->
                           exceptions.authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper))
                             .accessDeniedHandler(new RestAccessDeniedHandler(objectMapper)))
      .addFilterAfter(new CsrfCookieFilter(csrfTokenRepository), BasicAuthenticationFilter.class)
      .formLogin(AbstractHttpConfigurer::disable)
      .httpBasic(AbstractHttpConfigurer::disable)
      .logout(logout -> logout.logoutUrl("/api/auth/logout").invalidateHttpSession(true))
      .build();
  }

  @Bean
  PasswordEncoder passwordEncoder()
  {
    return new BCryptPasswordEncoder();
  }

  @Bean
  CookieCsrfTokenRepository csrfTokenRepository()
  {
    return CookieCsrfTokenRepository.withHttpOnlyFalse();
  }

  private static final class CsrfCookieFilter extends OncePerRequestFilter
  {

    private final CookieCsrfTokenRepository csrfTokenRepository;

    private CsrfCookieFilter(CookieCsrfTokenRepository csrfTokenRepository)
    {
      this.csrfTokenRepository = csrfTokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException
    {
      CsrfToken csrfToken = csrfTokenRepository.loadDeferredToken(request, response).get();
      if (csrfToken != null)
      {
        csrfToken.getToken();
      }
      filterChain.doFilter(request, response);
    }
  }

  private static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler
  {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken)
    {
      xor.handle(request, response, csrfToken);
      csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken)
    {
      if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName())))
      {
        return plain.resolveCsrfTokenValue(request, csrfToken);
      }
      return xor.resolveCsrfTokenValue(request, csrfToken);
    }
  }
}
