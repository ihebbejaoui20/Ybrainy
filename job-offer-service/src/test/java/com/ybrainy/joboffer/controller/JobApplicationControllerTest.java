package com.ybrainy.joboffer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ybrainy.joboffer.client.PartnershipClient;
import com.ybrainy.joboffer.dto.*;
import com.ybrainy.joboffer.entity.ApplicationStatus;
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

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {JobOfferController.class, JobApplicationController.class}, excludeAutoConfiguration = RabbitAutoConfiguration.class)
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
class JobApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobApplicationService service;

    @MockBean
    private JobOfferService jobOfferService;

    @MockBean
    private PartnershipClient partnershipClient;

    @Autowired
    private ObjectMapper objectMapper;

    // ───────── CREATE ─────────

    @Test
    void create_shouldReturn201() throws Exception {
        JobApplicationRequest request = new JobApplicationRequest(
                "Alice", "alice@test.com", "Motivation", null
        );

        when(service.create(eq("offer-1"), any()))
                .thenReturn(response("app-1"));

        mockMvc.perform(post("/api/offers/offer-1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("app-1"));
    }

    // ───────── LIST ─────────

    @Test
    void listByOffer_shouldReturnList() throws Exception {
        when(service.listByOffer("offer-1"))
                .thenReturn(List.of(response("app-1")));

        mockMvc.perform(get("/api/offers/offer-1/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ───────── UPDATE ─────────

    @Test
    void updateReview_shouldReturn200() throws Exception {
        JobApplicationUpdateRequest request =
                new JobApplicationUpdateRequest(ApplicationStatus.ACCEPTED, null);

        when(service.updateReview(eq("app-1"), any()))
                .thenReturn(response("app-1"));

        mockMvc.perform(put("/api/applications/app-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ───────── DELETE ─────────

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(service).delete("app-1");

        mockMvc.perform(delete("/api/applications/app-1"))
                .andExpect(status().isNoContent());
    }

    // ───────── ERROR ─────────

    @Test
    void getApplications_whenOfferNotFound_shouldReturn404() throws Exception {
        when(service.listByOffer("missing"))
                .thenThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(get("/api/offers/missing/applications"))
                .andExpect(status().isNotFound());
    }

    // ───────── HELPERS ─────────

    private JobApplicationResponse response(String id) {
        return new JobApplicationResponse(
                id,
                "offer-1",
                "Alice",
                "alice@test.com",
                "Motivation",
                null,
                ApplicationStatus.PENDING,
                null,
                Instant.now()
        );
    }
}
