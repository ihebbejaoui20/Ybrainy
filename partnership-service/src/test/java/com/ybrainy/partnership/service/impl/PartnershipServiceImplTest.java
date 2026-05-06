package com.ybrainy.partnership.service.impl;

import com.ybrainy.partnership.dto.PartnershipRequest;
import com.ybrainy.partnership.dto.PartnershipResponse;
import com.ybrainy.partnership.entity.Partnership;
import com.ybrainy.partnership.exception.BusinessException;
import com.ybrainy.partnership.exception.ResourceNotFoundException;
import com.ybrainy.partnership.mapper.PartnershipMapper;
import com.ybrainy.partnership.messaging.PartnershipEventPublisher;
import com.ybrainy.partnership.repository.PartnershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartnershipServiceImplTest {

    @Mock
    private PartnershipRepository repository;

    @Mock
    private PartnershipMapper mapper;

    @Mock
    private PartnershipEventPublisher eventPublisher;

    @InjectMocks
    private PartnershipServiceImpl service;

    private Partnership partnership;
    private PartnershipRequest request;
    private PartnershipResponse response;

    @BeforeEach
    void setUp() {
        partnership = new Partnership();
        partnership.setId("abc-123");
        partnership.setName("Tech Corp");
        partnership.setEmail("tech@corp.com");
        partnership.setPhone("+21600000000");
        partnership.setWebsite("https://techcorp.com");
        partnership.setDescription("A tech company");
        partnership.setActive(true);
        partnership.setCreatedAt(Instant.now());
        partnership.setUpdatedAt(Instant.now());

        request = new PartnershipRequest(
                "Tech Corp",
                "tech@corp.com",
                "+21600000000",
                "https://techcorp.com",
                "A tech company",
                true
        );

        response = new PartnershipResponse(
                "abc-123",
                "Tech Corp",
                "tech@corp.com",
                "+21600000000",
                "https://techcorp.com",
                "A tech company",
                true,
                Instant.now(),
                Instant.now()
        );
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Test
    void create_shouldSaveAndReturnResponse_whenEmailIsNew() {
        when(repository.existsByEmailIgnoreCase("tech@corp.com")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(partnership);
        when(repository.save(partnership)).thenReturn(partnership);
        when(mapper.toResponse(partnership)).thenReturn(response);

        PartnershipResponse result = service.create(request);

        assertThat(result).isEqualTo(response);
        verify(repository).save(partnership);
        verify(eventPublisher).publishCreated(partnership);
    }

    @Test
    void create_shouldThrowBusinessException_whenEmailAlreadyExists() {
        when(repository.existsByEmailIgnoreCase("tech@corp.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("partnership already exists");

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishCreated(any());
    }

    @Test
    void create_shouldNormalizeEmail_beforeCheckingDuplicate() {
        PartnershipRequest requestWithUpperEmail = new PartnershipRequest(
                "Tech Corp", "TECH@CORP.COM", null, null, null, true
        );
        when(repository.existsByEmailIgnoreCase("tech@corp.com")).thenReturn(false);
        when(mapper.toEntity(requestWithUpperEmail)).thenReturn(partnership);
        when(repository.save(partnership)).thenReturn(partnership);
        when(mapper.toResponse(partnership)).thenReturn(response);

        service.create(requestWithUpperEmail);

        verify(repository).existsByEmailIgnoreCase("tech@corp.com");
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @Test
    void update_shouldUpdateAndReturnResponse_whenValid() {
        when(repository.findById("abc-123")).thenReturn(Optional.of(partnership));
        when(repository.existsByEmailIgnoreCaseAndIdNot("tech@corp.com", "abc-123")).thenReturn(false);
        when(repository.save(partnership)).thenReturn(partnership);
        when(mapper.toResponse(partnership)).thenReturn(response);

        PartnershipResponse result = service.update("abc-123", request);

        assertThat(result).isEqualTo(response);
        verify(mapper).apply(partnership, request);
        verify(eventPublisher).publishUpdated(partnership);
    }

    @Test
    void update_shouldThrowResourceNotFoundException_whenIdNotFound() {
        when(repository.findById("unknown-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("unknown-id", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Partnership not found");

        verify(repository, never()).save(any());
    }

    @Test
    void update_shouldThrowBusinessException_whenEmailUsedByAnother() {
        when(repository.findById("abc-123")).thenReturn(Optional.of(partnership));
        when(repository.existsByEmailIgnoreCaseAndIdNot("tech@corp.com", "abc-123")).thenReturn(true);

        assertThatThrownBy(() -> service.update("abc-123", request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Another partnership already uses this email");

        verify(repository, never()).save(any());
    }

    // ─── GET BY ID ────────────────────────────────────────────────────────────

    @Test
    void getById_shouldReturnResponse_whenFound() {
        when(repository.findById("abc-123")).thenReturn(Optional.of(partnership));
        when(mapper.toResponse(partnership)).thenReturn(response);

        PartnershipResponse result = service.getById("abc-123");

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getById_shouldThrowResourceNotFoundException_whenNotFound() {
        when(repository.findById("not-found")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("not-found"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Partnership not found: not-found");
    }

    // ─── GET ALL ──────────────────────────────────────────────────────────────

    @Test
    void getAll_shouldReturnAllPartnerships_whenSearchIsNull() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Partnership> page = new PageImpl<>(List.of(partnership));
        when(repository.findAll(pageable)).thenReturn(page);
        when(mapper.toResponse(partnership)).thenReturn(response);

        Page<PartnershipResponse> result = service.getAll(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(repository).findAll(pageable);
        verify(repository, never()).findByNameContainingIgnoreCase(any(), any());
    }

    @Test
    void getAll_shouldReturnAllPartnerships_whenSearchIsBlank() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Partnership> page = new PageImpl<>(List.of(partnership));
        when(repository.findAll(pageable)).thenReturn(page);
        when(mapper.toResponse(partnership)).thenReturn(response);

        Page<PartnershipResponse> result = service.getAll("   ", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(repository).findAll(pageable);
    }

    @Test
    void getAll_shouldFilterByName_whenSearchIsProvided() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Partnership> page = new PageImpl<>(List.of(partnership));
        when(repository.findByNameContainingIgnoreCase("tech", pageable)).thenReturn(page);
        when(mapper.toResponse(partnership)).thenReturn(response);

        Page<PartnershipResponse> result = service.getAll("tech", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(repository).findByNameContainingIgnoreCase("tech", pageable);
        verify(repository, never()).findAll(any(Pageable.class));
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @Test
    void delete_shouldDeleteAndPublishEvent_whenFound() {
        when(repository.findById("abc-123")).thenReturn(Optional.of(partnership));

        service.delete("abc-123");

        verify(repository).delete(partnership);
        verify(eventPublisher).publishDeleted(partnership);
    }

    @Test
    void delete_shouldThrowResourceNotFoundException_whenNotFound() {
        when(repository.findById("not-found")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("not-found"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).delete(any());
        verify(eventPublisher, never()).publishDeleted(any());
    }

    // ─── EXISTS BY ID ─────────────────────────────────────────────────────────

    @Test
    void existsById_shouldReturnTrue_whenExists() {
        when(repository.existsById("abc-123")).thenReturn(true);

        assertThat(service.existsById("abc-123")).isTrue();
    }

    @Test
    void existsById_shouldReturnFalse_whenNotExists() {
        when(repository.existsById("unknown")).thenReturn(false);

        assertThat(service.existsById("unknown")).isFalse();
    }
}