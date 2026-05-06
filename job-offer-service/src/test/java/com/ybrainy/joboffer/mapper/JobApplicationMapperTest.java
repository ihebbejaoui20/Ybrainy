package com.ybrainy.joboffer.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.ybrainy.joboffer.dto.JobApplicationRequest;
import com.ybrainy.joboffer.dto.JobApplicationResponse;
import com.ybrainy.joboffer.dto.JobApplicationUpdateRequest;
import com.ybrainy.joboffer.entity.ApplicationStatus;
import com.ybrainy.joboffer.entity.JobApplication;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JobApplicationMapperTest {

  private final JobApplicationMapper mapper = new JobApplicationMapper();

  @Test
  void toEntity_shouldTrimAndNormalizeFields() {
    JobApplicationRequest request =
        new JobApplicationRequest("  Alice  ", "  ALICE@EXAMPLE.COM  ", "  Motivation  ", "cv-data");

    JobApplication entity = mapper.toEntity("  offer-1  ", request);

    assertEquals("offer-1", entity.getOfferId());
    assertEquals("Alice", entity.getApplicantName());
    assertEquals("alice@example.com", entity.getApplicantEmail());
    assertEquals("Motivation", entity.getMessage());
    assertEquals("cv-data", entity.getCvDataUrl());
    assertEquals(ApplicationStatus.PENDING, entity.getStatus());
    assertNull(entity.getReviewerNotes());
  }

  @Test
  void toEntity_shouldSupportNullMessage() {
    JobApplicationRequest request =
        new JobApplicationRequest("Alice", "alice@example.com", null, null);

    JobApplication entity = mapper.toEntity("offer-1", request);

    assertNull(entity.getMessage());
    assertNull(entity.getCvDataUrl());
  }

  @Test
  void applyReview_shouldMapStatusAndTrimNotes() {
    JobApplication app = new JobApplication();
    app.setStatus(ApplicationStatus.PENDING);

    mapper.applyReview(app, new JobApplicationUpdateRequest(ApplicationStatus.ACCEPTED, "  Great profile  "));

    assertEquals(ApplicationStatus.ACCEPTED, app.getStatus());
    assertEquals("Great profile", app.getReviewerNotes());
  }

  @Test
  void applyReview_shouldAllowNullNotes() {
    JobApplication app = new JobApplication();

    mapper.applyReview(app, new JobApplicationUpdateRequest(ApplicationStatus.REJECTED, null));

    assertEquals(ApplicationStatus.REJECTED, app.getStatus());
    assertNull(app.getReviewerNotes());
  }

  @Test
  void toResponse_shouldMapAllFields() {
    JobApplication app = new JobApplication();
    Instant createdAt = Instant.now();
    app.setId("app-1");
    app.setOfferId("offer-1");
    app.setApplicantName("Alice");
    app.setApplicantEmail("alice@example.com");
    app.setMessage("Motivation");
    app.setCvDataUrl("cv-data");
    app.setStatus(ApplicationStatus.REVIEWED);
    app.setReviewerNotes("Good");
    app.setCreatedAt(createdAt);

    JobApplicationResponse response = mapper.toResponse(app);

    assertEquals("app-1", response.id());
    assertEquals("offer-1", response.offerId());
    assertEquals("Alice", response.applicantName());
    assertEquals("alice@example.com", response.applicantEmail());
    assertEquals("Motivation", response.message());
    assertEquals("cv-data", response.cvDataUrl());
    assertEquals(ApplicationStatus.REVIEWED, response.status());
    assertEquals("Good", response.reviewerNotes());
    assertEquals(createdAt, response.createdAt());
  }
}

