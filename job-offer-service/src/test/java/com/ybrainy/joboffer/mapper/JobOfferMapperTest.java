package com.ybrainy.joboffer.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.ybrainy.joboffer.dto.JobOfferRequest;
import com.ybrainy.joboffer.dto.JobOfferResponse;
import com.ybrainy.joboffer.entity.ContractType;
import com.ybrainy.joboffer.entity.JobOffer;
import com.ybrainy.joboffer.entity.OfferStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class JobOfferMapperTest {

  private final JobOfferMapper mapper = new JobOfferMapper();

  @Test
  void toEntity_shouldMapAndTrimFields() {
    JobOfferRequest request =
        new JobOfferRequest(
            "  Java Dev  ",
            "  Build APIs  ",
            "  Tunis  ",
            "data:image/png;base64,AAA",
            new BigDecimal("1000"),
            new BigDecimal("1500"),
            ContractType.CDI,
            OfferStatus.OPEN,
            LocalDate.now().plusDays(10),
            "  partner-1  ");

    JobOffer entity = mapper.toEntity(request);

    assertEquals("Java Dev", entity.getTitle());
    assertEquals("Build APIs", entity.getDescription());
    assertEquals("Tunis", entity.getLocation());
    assertEquals("data:image/png;base64,AAA", entity.getImageDataUrl());
    assertEquals(new BigDecimal("1000"), entity.getSalaryMin());
    assertEquals(new BigDecimal("1500"), entity.getSalaryMax());
    assertEquals(ContractType.CDI, entity.getContractType());
    assertEquals(OfferStatus.OPEN, entity.getStatus());
    assertEquals(request.deadline(), entity.getDeadline());
    assertEquals("partner-1", entity.getPartnershipId());
  }

  @Test
  void apply_shouldHandleNullLocation() {
    JobOffer target = new JobOffer();
    JobOfferRequest request =
        new JobOfferRequest(
            " Title ",
            " Desc ",
            null,
            null,
            null,
            null,
            ContractType.FREELANCE,
            OfferStatus.DRAFT,
            null,
            " partner ");

    mapper.apply(target, request);

    assertEquals("Title", target.getTitle());
    assertEquals("Desc", target.getDescription());
    assertNull(target.getLocation());
    assertNull(target.getImageDataUrl());
    assertEquals("partner", target.getPartnershipId());
  }

  @Test
  void toResponse_withoutPartnershipDetails_shouldMapCoreFields() {
    JobOffer offer = sampleOffer();

    JobOfferResponse response = mapper.toResponse(offer);

    assertEquals(offer.getId(), response.id());
    assertEquals(offer.getTitle(), response.title());
    assertEquals(offer.getPartnershipId(), response.partnershipId());
    assertNull(response.partnershipName());
    assertNull(response.partnershipEmail());
  }

  @Test
  void toResponse_withPartnershipDetails_shouldMapAllFields() {
    JobOffer offer = sampleOffer();

    JobOfferResponse response = mapper.toResponse(offer, "Partner Name", "partner@example.com");

    assertEquals("Partner Name", response.partnershipName());
    assertEquals("partner@example.com", response.partnershipEmail());
    assertEquals(offer.getCreatedAt(), response.createdAt());
    assertEquals(offer.getUpdatedAt(), response.updatedAt());
  }

  private static JobOffer sampleOffer() {
    JobOffer offer = new JobOffer();
    offer.setId("offer-1");
    offer.setTitle("Java Dev");
    offer.setDescription("Build APIs");
    offer.setLocation("Tunis");
    offer.setImageDataUrl("data:image/png;base64,AAA");
    offer.setSalaryMin(new BigDecimal("1000"));
    offer.setSalaryMax(new BigDecimal("1500"));
    offer.setContractType(ContractType.CDI);
    offer.setStatus(OfferStatus.OPEN);
    offer.setDeadline(LocalDate.now().plusDays(5));
    offer.setPartnershipId("partner-1");
    offer.setCreatedAt(Instant.now());
    offer.setUpdatedAt(Instant.now());
    return offer;
  }
}

