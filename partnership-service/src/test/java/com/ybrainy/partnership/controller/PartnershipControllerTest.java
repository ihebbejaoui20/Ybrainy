package com.ybrainy.partnership.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ybrainy.partnership.client.JobOfferClient;
import com.ybrainy.partnership.dto.JobOfferSummary;
import com.ybrainy.partnership.dto.PartnershipRequest;
import com.ybrainy.partnership.dto.PartnershipResponse;
import com.ybrainy.partnership.exception.ResourceNotFoundException;
import com.ybrainy.partnership.exception.BusinessException;
import com.ybrainy.partnership.service.PartnershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// FIX 1 : import manquant qui causait "Cannot resolve symbol 'PartnershipController'"
@WebMvcTest(PartnershipController.class)
class PartnershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PartnershipService service;

    @MockBean
    private JobOfferClient jobOfferClient;

    private PartnershipResponse response;
    private PartnershipRequest validRequest;

    @BeforeEach
    void setUp() {
        response = new PartnershipResponse(
                "abc-123", "Tech Corp", "tech@corp.com",
                "+21600000000", "https://techcorp.com",
                "A tech company", true, Instant.now(), Instant.now()
        );

        validRequest = new PartnershipRequest(
                "Tech Corp", "tech@corp.com",
                "+21600000000", "https://techcorp.com",
                "A tech company", true
        );
    }

    // ─── POST /api/partnerships ───────────────────────────────────────────────

    @Test
    void create_shouldReturn201_whenRequestIsValid() throws Exception {
        when(service.create(any(PartnershipRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/partnerships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("abc-123"))
                .andExpect(jsonPath("$.name").value("Tech Corp"))
                .andExpect(jsonPath("$.email").value("tech@corp.com"));
    }

    @Test
    void create_shouldReturn400_whenNameIsBlank() throws Exception {
        PartnershipRequest invalid = new PartnershipRequest(
                "", "tech@corp.com", null, null, null, true
        );

        mockMvc.perform(post("/api/partnerships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400_whenEmailIsInvalid() throws Exception {
        PartnershipRequest invalid = new PartnershipRequest(
                "Tech Corp", "not-an-email", null, null, null, true
        );

        mockMvc.perform(post("/api/partnerships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400_whenEmailAlreadyExists() throws Exception {
        when(service.create(any())).thenThrow(new BusinessException("A partnership already exists with this email"));

        mockMvc.perform(post("/api/partnerships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    // ─── PUT /api/partnerships/{id} ───────────────────────────────────────────

    @Test
    void update_shouldReturn200_whenRequestIsValid() throws Exception {
        when(service.update(eq("abc-123"), any(PartnershipRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/partnerships/abc-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("abc-123"));
    }

    @Test
    void update_shouldReturn404_whenPartnershipNotFound() throws Exception {
        when(service.update(eq("not-found"), any()))
                .thenThrow(new ResourceNotFoundException("Partnership not found: not-found"));

        mockMvc.perform(put("/api/partnerships/not-found")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound());
    }

    // ─── GET /api/partnerships/{id} ───────────────────────────────────────────

    @Test
    void getById_shouldReturn200_whenFound() throws Exception {
        when(service.getById("abc-123")).thenReturn(response);

        mockMvc.perform(get("/api/partnerships/abc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("abc-123"))
                .andExpect(jsonPath("$.email").value("tech@corp.com"));
    }

    @Test
    void getById_shouldReturn404_whenNotFound() throws Exception {
        when(service.getById("not-found"))
                .thenThrow(new ResourceNotFoundException("Partnership not found: not-found"));

        mockMvc.perform(get("/api/partnerships/not-found"))
                .andExpect(status().isNotFound());
    }

    // ─── GET /api/partnerships/{id}/with-offers ───────────────────────────────

    @Test
    void getByIdWithOffers_shouldReturn200_withOffersData() throws Exception {
        // FIX 2 : JobOfferSummary attend 11 arguments — adapter selon votre constructeur réel.
        // Remplacez les null par les vraies valeurs si nécessaire.
        JobOfferSummary offer = new JobOfferSummary(
                "offer-1",          // id
                "Software Engineer",// title
                "abc-123",          // partnershipId
                null,               // description
                null,               // location
                null,               // contractType
                null,               // salaryRange
                null,               // experienceLevel
                null,               // remote
                null,               // createdAt
                null                // status
        );

        when(service.getById("abc-123")).thenReturn(response);
        when(jobOfferClient.findByPartnership("abc-123", 0, 50)).thenReturn(List.of(offer));

        mockMvc.perform(get("/api/partnerships/abc-123/with-offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partnership.id").value("abc-123"))
                .andExpect(jsonPath("$.totalOffers").value(1))
                .andExpect(jsonPath("$.offers[0].id").value("offer-1"));
    }

    // ─── GET /api/partnerships ────────────────────────────────────────────────

    @Test
    void getAll_shouldReturn200_withDefaultPagination() throws Exception {
        Page<PartnershipResponse> page = new PageImpl<>(List.of(response));
        when(service.getAll(isNull(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/partnerships"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("abc-123"));
    }

    @Test
    void getAll_shouldReturn200_withSearchParam() throws Exception {
        Page<PartnershipResponse> page = new PageImpl<>(List.of(response));
        when(service.getAll(eq("tech"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/partnerships?search=tech"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ─── DELETE /api/partnerships/{id} ────────────────────────────────────────

    @Test
    void delete_shouldReturn204_whenDeleted() throws Exception {
        doNothing().when(service).delete("abc-123");

        mockMvc.perform(delete("/api/partnerships/abc-123"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Partnership not found: not-found"))
                .when(service).delete("not-found");

        mockMvc.perform(delete("/api/partnerships/not-found"))
                .andExpect(status().isNotFound());
    }

    // ─── GET /api/partnerships/{id}/exists ────────────────────────────────────

    @Test
    void exists_shouldReturnTrue_whenFound() throws Exception {
        when(service.existsById("abc-123")).thenReturn(true);

        mockMvc.perform(get("/api/partnerships/abc-123/exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    void exists_shouldReturnFalse_whenNotFound() throws Exception {
        when(service.existsById("unknown")).thenReturn(false);

        mockMvc.perform(get("/api/partnerships/unknown/exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }
}