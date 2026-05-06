package com.ybrainy.joboffer.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ybrainy.joboffer.dto.GenerateApplicationRequest;
import com.ybrainy.joboffer.dto.GenerateApplicationResponse;
import com.ybrainy.joboffer.service.ApplicationGenerationService;
import org.junit.jupiter.api.Test;

class ApplicationGenerationControllerTest {

  @Test
  void generate_shouldDelegateToService() {
    ApplicationGenerationService service = mock(ApplicationGenerationService.class);
    ApplicationGenerationController controller = new ApplicationGenerationController(service);

    GenerateApplicationRequest request =
        new GenerateApplicationRequest("cv", "job", "skeleton", null, null);
    GenerateApplicationResponse expected =
        new GenerateApplicationResponse("optimized", "cover", "data:image/png;base64,AAA", "ok");

    when(service.generate(request)).thenReturn(expected);

    GenerateApplicationResponse result = controller.generate(request);

    assertEquals(expected, result);
    verify(service).generate(request);
  }
}

