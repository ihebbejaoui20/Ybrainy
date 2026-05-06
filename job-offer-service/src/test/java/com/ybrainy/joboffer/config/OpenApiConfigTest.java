package com.ybrainy.joboffer.config;

import static org.junit.jupiter.api.Assertions.*;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

  @Test
  void jobOfferOpenAPI_shouldExposeApiMetadata() {
    OpenApiConfig config = new OpenApiConfig();

    OpenAPI openAPI = config.jobOfferOpenAPI();

    assertNotNull(openAPI);
    assertNotNull(openAPI.getInfo());
    assertEquals("Job Offer Service API", openAPI.getInfo().getTitle());
    assertEquals("1.0", openAPI.getInfo().getVersion());
    assertTrue(openAPI.getInfo().getDescription().contains("Offres d'emploi"));
  }
}

