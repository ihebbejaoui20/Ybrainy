package com.ybrainy.joboffer.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ybrainy.joboffer.client.PartnershipClient;
import com.ybrainy.joboffer.dto.ExistsResponse;
import com.ybrainy.joboffer.dto.JobOfferRequest;
import com.ybrainy.joboffer.dto.JobOfferResponse;
import com.ybrainy.joboffer.dto.PartnershipSummary;
import com.ybrainy.joboffer.entity.ContractType;
import com.ybrainy.joboffer.entity.JobOffer;
import com.ybrainy.joboffer.entity.OfferStatus;
import com.ybrainy.joboffer.exception.BusinessException;
import com.ybrainy.joboffer.exception.ResourceNotFoundException;
import com.ybrainy.joboffer.mapper.JobOfferMapper;
import com.ybrainy.joboffer.repository.JobOfferRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class JobOfferServiceImplTest {

  @Mock private JobOfferRepository repository;
  @Mock private JobOfferMapper mapper;
  @Mock private PartnershipClient partnershipClient;

  private JobOfferServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new JobOfferServiceImpl(repository, mapper, partnershipClient);
  }

  // ── create ────────────────────────────────────────────────────────────────

  @Test
  void create_whenValidRequest_savesAndReturnsResponse() {
    JobOfferRequest request = requestWithSalaries("partner-1", new BigDecimal("1000"), new BigDecimal("1500"));
    JobOffer entity = jobOffer("offer-1", "partner-1");
    JobOfferResponse response = responseFor("offer-1");

    when(partnershipClient.exists("partner-1")).thenReturn(new ExistsResponse(true));
    when(mapper.toEntity(request)).thenReturn(entity);
    when(repository.save(entity)).thenReturn(entity);
    when(mapper.toResponse(eq(entity), any(), any())).thenReturn(response);

    assertEquals(response, service.create(request));
    verify(repository).save(entity);
  }

  @Test
  void create_whenPartnershipServiceFails_throwsBusinessException() {
    JobOfferRequest request = requestWithSalaries("partner-1", new BigDecimal("1000"), new BigDecimal("1500"));
    when(partnershipClient.exists("partner-1")).thenThrow(new RuntimeException("down"));

    BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));

    assertTrue(ex.getMessage().contains("Unable to validate partnership"));
    verifyNoInteractions(mapper);
    verify(repository, never()).save(any());
  }

  @Test
  void create_whenPartnershipDoesNotExist_throwsBusinessException() {
    JobOfferRequest request = requestWithSalaries("partner-1", new BigDecimal("1000"), new BigDecimal("1500"));
    when(partnershipClient.exists("partner-1")).thenReturn(new ExistsResponse(false));

    BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));

    assertTrue(ex.getMessage().contains("Invalid partnershipId"));
    verifyNoInteractions(mapper);
    verify(repository, never()).save(any());
  }

  @Test
  void create_whenSalaryMinGreaterThanMax_throwsBusinessException() {
    JobOfferRequest request = requestWithSalaries("partner-1", new BigDecimal("2000"), new BigDecimal("1500"));
    when(partnershipClient.exists("partner-1")).thenReturn(new ExistsResponse(true));

    BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));

    assertTrue(ex.getMessage().contains("salaryMin"));
    verify(mapper, never()).toEntity(any());
    verify(repository, never()).save(any());
  }

  @Test
  void create_whenSalaryMinEqualsMax_doesNotThrow() {
    BigDecimal salary = new BigDecimal("1500");
    JobOfferRequest request = requestWithSalaries("partner-1", salary, salary);
    JobOffer entity = jobOffer("offer-1", "partner-1");
    JobOfferResponse response = responseFor("offer-1");

    when(partnershipClient.exists("partner-1")).thenReturn(new ExistsResponse(true));
    when(mapper.toEntity(request)).thenReturn(entity);
    when(repository.save(entity)).thenReturn(entity);
    when(mapper.toResponse(eq(entity), any(), any())).thenReturn(response);

    assertDoesNotThrow(() -> service.create(request));
  }

  @Test
  void create_whenSalaryMinIsNull_doesNotValidateSalary() {
    JobOfferRequest request = requestWithSalaries("partner-1", null, new BigDecimal("1500"));
    JobOffer entity = jobOffer("offer-1", "partner-1");
    JobOfferResponse response = responseFor("offer-1");

    when(partnershipClient.exists("partner-1")).thenReturn(new ExistsResponse(true));
    when(mapper.toEntity(request)).thenReturn(entity);
    when(repository.save(entity)).thenReturn(entity);
    when(mapper.toResponse(eq(entity), any(), any())).thenReturn(response);

    assertDoesNotThrow(() -> service.create(request));
  }

  @Test
  void create_whenSalaryMaxIsNull_doesNotValidateSalary() {
    JobOfferRequest request = requestWithSalaries("partner-1", new BigDecimal("1000"), null);
    JobOffer entity = jobOffer("offer-1", "partner-1");
    JobOfferResponse response = responseFor("offer-1");

    when(partnershipClient.exists("partner-1")).thenReturn(new ExistsResponse(true));
    when(mapper.toEntity(request)).thenReturn(entity);
    when(repository.save(entity)).thenReturn(entity);
    when(mapper.toResponse(eq(entity), any(), any())).thenReturn(response);

    assertDoesNotThrow(() -> service.create(request));
  }

  @Test
  void create_whenPartnershipClientReturnsData_enrichesResponse() {
    JobOfferRequest request = requestWithSalaries("partner-1", new BigDecimal("1000"), new BigDecimal("1500"));
    JobOffer entity = jobOffer("offer-1", "partner-1");
    JobOfferResponse response = responseFor("offer-1");

    when(partnershipClient.exists("partner-1")).thenReturn(new ExistsResponse(true));
    when(partnershipClient.getById("partner-1")).thenReturn(new PartnershipSummary("partner-1", "Acme", "acme@example.com"));
    when(mapper.toEntity(request)).thenReturn(entity);
    when(repository.save(entity)).thenReturn(entity);
    when(mapper.toResponse(entity, "Acme", "acme@example.com")).thenReturn(response);

    JobOfferResponse result = service.create(request);

    assertEquals(response, result);
    verify(mapper).toResponse(entity, "Acme", "acme@example.com");
  }

  @Test
  void create_whenPartnershipClientGetByIdReturnsNull_enrichesWithNulls() {
    JobOfferRequest request = requestWithSalaries("partner-1", new BigDecimal("1000"), new BigDecimal("1500"));
    JobOffer entity = jobOffer("offer-1", "partner-1");
    JobOfferResponse response = responseFor("offer-1");

    when(partnershipClient.exists("partner-1")).thenReturn(new ExistsResponse(true));
    when(partnershipClient.getById("partner-1")).thenReturn(null);
    when(mapper.toEntity(request)).thenReturn(entity);
    when(repository.save(entity)).thenReturn(entity);
    when(mapper.toResponse(entity, null, null)).thenReturn(response);

    service.create(request);

    verify(mapper).toResponse(entity, null, null);
  }

  @Test
  void create_whenPartnershipGetByIdThrows_failsSoftlyWithNullNames() {
    JobOfferRequest request = requestWithSalaries("partner-1", new BigDecimal("1000"), new BigDecimal("1500"));
    JobOffer entity = jobOffer("offer-1", "partner-1");
    JobOfferResponse response = responseFor("offer-1");

    when(partnershipClient.exists("partner-1")).thenReturn(new ExistsResponse(true));
    when(partnershipClient.getById("partner-1")).thenThrow(new RuntimeException("service down"));
    when(mapper.toEntity(request)).thenReturn(entity);
    when(repository.save(entity)).thenReturn(entity);
    when(mapper.toResponse(entity, null, null)).thenReturn(response);

    assertDoesNotThrow(() -> service.create(request));
    verify(mapper).toResponse(entity, null, null);
  }

  // ── update ────────────────────────────────────────────────────────────────

  @Test
  void update_whenValidRequest_updatesAndReturnsResponse() {
    JobOfferRequest request = requestWithSalaries("partner-1", new BigDecimal("1000"), new BigDecimal("1500"));
    JobOffer entity = jobOffer("offer-1", "partner-1");
    JobOfferResponse response = responseFor("offer-1");

    when(partnershipClient.exists("partner-1")).thenReturn(new ExistsResponse(true));
    when(repository.findById("offer-1")).thenReturn(Optional.of(entity));
    when(repository.save(entity)).thenReturn(entity);
    when(mapper.toResponse(eq(entity), any(), any())).thenReturn(response);

    assertEquals(response, service.update("offer-1", request));
    verify(mapper).apply(entity, request);
    verify(repository).save(entity);
  }

  @Test
  void update_whenOfferNotFound_throwsResourceNotFoundException() {
    JobOfferRequest request = requestWithSalaries("partner-1", new BigDecimal("1000"), new BigDecimal("1500"));
    when(partnershipClient.exists("partner-1")).thenReturn(new ExistsResponse(true));
    when(repository.findById("missing-id")).thenReturn(Optional.empty());

    ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
            () -> service.update("missing-id", request));

    assertTrue(ex.getMessage().contains("Job offer not found"));
    verify(mapper, never()).apply(any(), any());
    verify(repository, never()).save(any());
  }

  @Test
  void update_whenSalaryInvalid_throwsBeforeLookup() {
    JobOfferRequest request = requestWithSalaries("partner-1", new BigDecimal("3000"), new BigDecimal("1000"));
    when(partnershipClient.exists("partner-1")).thenReturn(new ExistsResponse(true));

    assertThrows(BusinessException.class, () -> service.update("offer-1", request));

    verify(repository, never()).findById(any());
  }

  // ── getById ───────────────────────────────────────────────────────────────

  @Test
  void getById_whenExists_returnsResponse() {
    JobOffer entity = jobOffer("offer-1", "partner-1");
    JobOfferResponse response = responseFor("offer-1");

    when(repository.findById("offer-1")).thenReturn(Optional.of(entity));
    when(mapper.toResponse(eq(entity), any(), any())).thenReturn(response);

    assertEquals(response, service.getById("offer-1"));
  }

  @Test
  void getById_whenNotFound_throwsResourceNotFoundException() {
    when(repository.findById("missing")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.getById("missing"));
  }

  // ── getAll ────────────────────────────────────────────────────────────────

  @Test
  void getAll_whenPartnershipIdProvided_usesPartnershipRepositorySearch() {
    Pageable pageable = PageRequest.of(0, 10);
    JobOffer entity = jobOffer("offer-1", "partner-1");
    JobOfferResponse response = responseFor("offer-1");

    when(repository.findByPartnershipId("partner-1", pageable)).thenReturn(new PageImpl<>(List.of(entity)));
    when(mapper.toResponse(eq(entity), any(), any())).thenReturn(response);

    Page<JobOfferResponse> page = service.getAll("partner-1", null, null, pageable);

    assertEquals(1, page.getTotalElements());
    verify(repository).findByPartnershipId("partner-1", pageable);
    verify(repository, never()).findAll(any(Pageable.class));
  }

  @Test
  void getAll_whenStatusProvided_usesStatusRepositorySearch() {
    Pageable pageable = PageRequest.of(0, 10);
    JobOffer entity = jobOffer("offer-1", "partner-1");
    JobOfferResponse response = responseFor("offer-1");

    when(repository.findByStatus(OfferStatus.OPEN, pageable)).thenReturn(new PageImpl<>(List.of(entity)));
    when(mapper.toResponse(eq(entity), any(), any())).thenReturn(response);

    Page<JobOfferResponse> page = service.getAll(null, OfferStatus.OPEN, null, pageable);

    assertEquals(1, page.getTotalElements());
    verify(repository).findByStatus(OfferStatus.OPEN, pageable);
  }

  @Test
  void getAll_whenKeywordProvided_usesKeywordRepositorySearch() {
    Pageable pageable = PageRequest.of(0, 10);
    JobOffer entity = jobOffer("offer-1", "partner-1");
    JobOfferResponse response = responseFor("offer-1");

    when(repository.findByTitleContainingIgnoreCase("java", pageable)).thenReturn(new PageImpl<>(List.of(entity)));
    when(mapper.toResponse(eq(entity), any(), any())).thenReturn(response);

    Page<JobOfferResponse> page = service.getAll(null, null, "  java  ", pageable);

    assertEquals(1, page.getTotalElements());
    verify(repository).findByTitleContainingIgnoreCase("java", pageable);
    verify(repository, never()).findAll(any(Pageable.class));
  }

  @Test
  void getAll_whenNoFilter_usesDefaultFindAll() {
    Pageable pageable = PageRequest.of(0, 10);
    when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

    Page<JobOfferResponse> page = service.getAll(null, null, null, pageable);

    assertTrue(page.isEmpty());
    verify(repository).findAll(pageable);
  }

  @Test
  void getAll_whenBlankPartnershipId_usesDefaultFindAll() {
    Pageable pageable = PageRequest.of(0, 10);
    when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

    service.getAll("   ", null, null, pageable);

    verify(repository).findAll(pageable);
  }

  @Test
  void getAll_whenBlankKeyword_usesDefaultFindAll() {
    Pageable pageable = PageRequest.of(0, 10);
    when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

    service.getAll(null, null, "   ", pageable);

    verify(repository).findAll(pageable);
  }

  // ── delete ────────────────────────────────────────────────────────────────

  @Test
  void delete_whenOfferExists_deletesEntity() {
    JobOffer entity = jobOffer("offer-1", "partner-1");
    when(repository.findById("offer-1")).thenReturn(Optional.of(entity));

    service.delete("offer-1");

    verify(repository).delete(entity);
  }

  @Test
  void delete_whenOfferNotFound_throwsResourceNotFoundException() {
    when(repository.findById("missing")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.delete("missing"));

    verify(repository, never()).delete(any());
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  private static JobOfferRequest requestWithSalaries(String partnershipId, BigDecimal salaryMin, BigDecimal salaryMax) {
    return new JobOfferRequest(
            "Java Developer",
            "Build and maintain backend services.",
            "Tunis",
            null,
            salaryMin,
            salaryMax,
            ContractType.CDI,
            OfferStatus.OPEN,
            LocalDate.now().plusDays(5),
            partnershipId);
  }

  private static JobOffer jobOffer(String id, String partnershipId) {
    JobOffer offer = new JobOffer();
    offer.setId(id);
    offer.setPartnershipId(partnershipId);
    offer.setTitle("Java Developer");
    offer.setCreatedAt(Instant.now());
    offer.setUpdatedAt(Instant.now());
    return offer;
  }

  private static JobOfferResponse responseFor(String id) {
    return new JobOfferResponse(
            id,
            "Java Developer",
            "Build and maintain backend services.",
            "Tunis",
            null,
            new BigDecimal("1000"),
            new BigDecimal("1500"),
            ContractType.CDI,
            OfferStatus.OPEN,
            LocalDate.now().plusDays(5),
            "partner-1",
            "Partner Name",
            "partner@example.com",
            Instant.now(),
            Instant.now());
  }
}