package com.ybrainy.joboffer.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class JobApplicationTest {

  @Test
  void onCreate_shouldGenerateDefaultsAndNormalizeEmail() {
    JobApplication application = new JobApplication();
    application.setApplicantEmail("  ALICE@EXAMPLE.COM  ");

    application.onCreate();

    assertNotNull(application.getId());
    assertFalse(application.getId().isBlank());
    assertNotNull(application.getCreatedAt());
    assertEquals("alice@example.com", application.getApplicantEmail());
    assertEquals(ApplicationStatus.PENDING, application.getStatus());
  }

  @Test
  void onCreate_shouldKeepExistingIdAndStatus() {
    JobApplication application = new JobApplication();
    application.setId("app-1");
    application.setStatus(ApplicationStatus.ACCEPTED);

    application.onCreate();

    assertEquals("app-1", application.getId());
    assertEquals(ApplicationStatus.ACCEPTED, application.getStatus());
    assertNotNull(application.getCreatedAt());
  }

  @Test
  void onCreate_shouldAllowNullEmail() {
    JobApplication application = new JobApplication();
    application.setApplicantEmail(null);

    application.onCreate();

    assertNull(application.getApplicantEmail());
  }

  @Test
  void gettersAndSetters_shouldExposeAllFields() {
    JobApplication application = new JobApplication();
    Instant createdAt = Instant.now();

    application.setId("app-1");
    application.setOfferId("offer-1");
    application.setApplicantName("Alice");
    application.setApplicantEmail("alice@example.com");
    application.setMessage("Motivation");
    application.setCvDataUrl("cv-data");
    application.setStatus(ApplicationStatus.REVIEWED);
    application.setReviewerNotes("Looks good");
    application.setCreatedAt(createdAt);

    assertEquals("app-1", application.getId());
    assertEquals("offer-1", application.getOfferId());
    assertEquals("Alice", application.getApplicantName());
    assertEquals("alice@example.com", application.getApplicantEmail());
    assertEquals("Motivation", application.getMessage());
    assertEquals("cv-data", application.getCvDataUrl());
    assertEquals(ApplicationStatus.REVIEWED, application.getStatus());
    assertEquals("Looks good", application.getReviewerNotes());
    assertEquals(createdAt, application.getCreatedAt());
  }
}

