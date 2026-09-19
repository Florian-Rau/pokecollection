package com.pokemoncollection.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class PokeApiClientConfig
{
  @Bean
  @ConditionalOnMissingBean(RestClient.Builder.class)
  RestClient.Builder restClientBuilder()
  {
    return RestClient.builder();
  }

  @Bean
  RestClient pokeApiRestClient(RestClient.Builder restClientBuilder,
                               @Value("${pokeapi.base-url:https://pokeapi.co/api/v2}") String pokeApiBaseUrl)
  {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(3000);
    requestFactory.setReadTimeout(3000);

    return restClientBuilder.baseUrl(pokeApiBaseUrl).requestFactory(requestFactory).build();
  }
}
