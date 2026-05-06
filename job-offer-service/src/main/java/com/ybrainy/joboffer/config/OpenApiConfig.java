package com.ybrainy.joboffer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI jobOfferOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Job Offer Service API")
                .version("1.0")
                .description("Offres d'emploi, candidatures et génération de CV / lettre."));
  }
}
