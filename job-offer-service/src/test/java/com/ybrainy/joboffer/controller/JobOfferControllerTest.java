package com.ybrainy.joboffer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ybrainy.joboffer.client.PartnershipClient;
import com.ybrainy.joboffer.dto.JobOfferRequest;
import com.ybrainy.joboffer.dto.JobOfferResponse;
import com.ybrainy.joboffer.entity.ContractType;
import com.ybrainy.joboffer.entity.OfferStatus;
import com.ybrainy.joboffer.exception.BusinessException;
import com.ybrainy.joboffer.exception.GlobalExceptionHandler;
import com.ybrainy.joboffer.exception.ResourceNotFoundException;
import com.ybrainy.joboffer.service.JobApplicationService;
import com.ybrainy.joboffer.service.JobOfferService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = JobOfferController.class, excludeAutoConfiguration = RabbitAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "app.rabbitmq.exchange=test-exchange",
        "app.rabbitmq.partnership.queue=test-partnership-queue",
        "app.rabbitmq.partnership.routing-key=test-partnership-key",
        "app.rabbitmq.application.queue=test-application-queue",
        "app.rabbitmq.application.routing-key=test-application-key",
        "app.rabbitmq.application.analytics-queue=test-analytics-queue"
})
class JobOfferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobOfferService service;

    @MockBean
    private JobApplicationService applicationService;

    @MockBean
    private PartnershipClient partnershipClient;

    @Autowired
    private ObjectMapper objectMapper;

    // ───────── CREATE ─────────

    @Test
    void create_shouldReturn201() throws Exception {
        JobOfferRequest request = request();
        JobOfferResponse response = response("offer-1");

        when(service.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("offer-1"));
    }

    @Test
    void create_whenInvalid_shouldReturn400() throws Exception {
        when(service.create(any()))
                .thenThrow(new BusinessException("Invalid"));

        mockMvc.perform(post("/api/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ───────── GET BY ID ─────────

    @Test
    void getById_shouldReturn200() throws Exception {
        when(service.getById("offer-1"))
                .thenReturn(response("offer-1"));

        mockMvc.perform(get("/api/offers/offer-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("offer-1"));
    }

    @Test
    void getById_shouldReturn404() throws Exception {
        when(service.getById("missing"))
                .thenThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(get("/api/offers/missing"))
                .andExpect(status().isNotFound());
    }

    // ───────── UPDATE ─────────

    @Test
    void update_shouldReturn200() throws Exception {
        when(service.update(eq("offer-1"), any()))
                .thenReturn(response("offer-1"));

        mockMvc.perform(put("/api/offers/offer-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isOk());
    }

    // ───────── DELETE ─────────

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(service).delete("offer-1");

        mockMvc.perform(delete("/api/offers/offer-1"))
                .andExpect(status().isNoContent());
    }

    // ───────── HELPERS ─────────

    private JobOfferRequest request() {
        return new JobOfferRequest(
                "Java Dev",
                "Desc",
                "Tunis",
                null,
                new BigDecimal("1000"),
                new BigDecimal("1500"),
                ContractType.CDI,
                OfferStatus.OPEN,
                LocalDate.now().plusDays(5),
                "partner-1"
        );
    }

    private JobOfferResponse response(String id) {
        return new JobOfferResponse(
                id,
                "Java Dev",
                "Desc",
                "Tunis",
                null,
                new BigDecimal("1000"),
                new BigDecimal("1500"),
                ContractType.CDI,
                OfferStatus.OPEN,
                LocalDate.now().plusDays(5),
                "partner-1",
                "Partner",
                "email@test.com",
                Instant.now(),
                Instant.now()
        );
    }
}
