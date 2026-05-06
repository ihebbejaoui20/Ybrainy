package com.ybrainy.joboffer.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class JobOfferTest {

  @Test
  void onCreate_shouldGenerateDefaultsWhenMissing() {
    JobOffer offer = new JobOffer();

    offer.onCreate();

    assertNotNull(offer.getId());
    assertFalse(offer.getId().isBlank());
    assertNotNull(offer.getCreatedAt());
    assertNotNull(offer.getUpdatedAt());
    assertEquals(offer.getCreatedAt(), offer.getUpdatedAt());
    assertEquals(OfferStatus.DRAFT, offer.getStatus());
  }

  @Test
  void onCreate_shouldKeepExistingIdAndStatus() {
    JobOffer offer = new JobOffer();
    offer.setId("offer-1");
    offer.setStatus(OfferStatus.OPEN);

    offer.onCreate();

    assertEquals("offer-1", offer.getId());
    assertEquals(OfferStatus.OPEN, offer.getStatus());
    assertNotNull(offer.getCreatedAt());
    assertNotNull(offer.getUpdatedAt());
  }

  @Test
  void onUpdate_shouldRefreshUpdatedAt() {
    JobOffer offer = new JobOffer();
    offer.setUpdatedAt(Instant.EPOCH);

    offer.onUpdate();

    assertTrue(offer.getUpdatedAt().isAfter(Instant.EPOCH));
  }

  @Test
  void gettersAndSetters_shouldExposeAllFields() {
    JobOffer offer = new JobOffer();
    Instant createdAt = Instant.now().minusSeconds(60);
    Instant updatedAt = Instant.now();
    LocalDate deadline = LocalDate.now().plusDays(20);

    offer.setId("offer-1");
    offer.setTitle("Java Dev");
    offer.setDescription("Build APIs");
    offer.setLocation("Tunis");
    offer.setSalaryMin(new BigDecimal("1000"));
    offer.setSalaryMax(new BigDecimal("1500"));
    offer.setContractType(ContractType.CDI);
    offer.setStatus(OfferStatus.OPEN);
    offer.setDeadline(deadline);
    offer.setPartnershipId("partner-1");
    offer.setImageDataUrl("data:image/png;base64,AAA");
    offer.setCreatedAt(createdAt);
    offer.setUpdatedAt(updatedAt);

    assertEquals("offer-1", offer.getId());
    assertEquals("Java Dev", offer.getTitle());
    assertEquals("Build APIs", offer.getDescription());
    assertEquals("Tunis", offer.getLocation());
    assertEquals(new BigDecimal("1000"), offer.getSalaryMin());
    assertEquals(new BigDecimal("1500"), offer.getSalaryMax());
    assertEquals(ContractType.CDI, offer.getContractType());
    assertEquals(OfferStatus.OPEN, offer.getStatus());
    assertEquals(deadline, offer.getDeadline());
    assertEquals("partner-1", offer.getPartnershipId());
    assertEquals("data:image/png;base64,AAA", offer.getImageDataUrl());
    assertEquals(createdAt, offer.getCreatedAt());
    assertEquals(updatedAt, offer.getUpdatedAt());
  }
}

