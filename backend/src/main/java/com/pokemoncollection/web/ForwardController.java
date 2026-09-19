package com.pokemoncollection.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards direct navigation/refresh on Angular client-side routes to the SPA's
 * {@code index.html} so the router can take over, instead of Spring MVC returning 404
 * for paths that only exist in the frontend router.
 */
@Controller
public class ForwardController
{
  @GetMapping({"/register", "/login", "/collection", "/browse"})
  public String forwardToIndex()
  {
    return "forward:/index.html";
  }
}
