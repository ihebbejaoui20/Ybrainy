package com.ybrainy.joboffer.messaging;

import static org.mockito.Mockito.*;

import com.ybrainy.joboffer.entity.ApplicationStatus;
import com.ybrainy.joboffer.entity.JobApplication;
import com.ybrainy.joboffer.repository.JobApplicationRepository;
import com.ybrainy.joboffer.service.JobApplicationNotificationService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobApplicationEventListenerTest {

  @Mock private JobApplicationRepository jobApplicationRepository;
  @Mock private JobApplicationNotificationService notificationService;

  private JobApplicationEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new JobApplicationEventListener(jobApplicationRepository, notificationService);
  }

  @Test
  void handleJobApplicationEvent_shouldIgnoreNullOrInvalidEvents() {
    listener.handleJobApplicationEvent(null);
    listener.handleJobApplicationEvent(event("APPLICATION_CREATED", "   ", ApplicationStatus.PENDING));

    verifyNoInteractions(jobApplicationRepository, notificationService);
  }

  @Test
  void handleJobApplicationEvent_shouldIgnoreCreatedEvents() {
    listener.handleJobApplicationEvent(event("APPLICATION_CREATED", "app-1", ApplicationStatus.PENDING));

    verifyNoInteractions(jobApplicationRepository, notificationService);
  }

  @Test
  void handleJobApplicationEvent_shouldIgnoreNonAcceptedStatusChanges() {
    listener.handleJobApplicationEvent(event("APPLICATION_STATUS_CHANGED", "app-1", ApplicationStatus.REJECTED));
    listener.handleJobApplicationEvent(event("OTHER_EVENT", "app-1", ApplicationStatus.ACCEPTED));

    verifyNoInteractions(jobApplicationRepository, notificationService);
  }

  @Test
  void handleJobApplicationEvent_shouldIgnoreMissingApplication() {
    when(jobApplicationRepository.findById("app-1")).thenReturn(Optional.empty());

    listener.handleJobApplicationEvent(event("APPLICATION_STATUS_CHANGED", "app-1", ApplicationStatus.ACCEPTED));

    verify(jobApplicationRepository).findById("app-1");
    verifyNoInteractions(notificationService);
  }

  @Test
  void handleJobApplicationEvent_shouldCallNotificationServiceForAcceptedStatus() {
    JobApplication application = new JobApplication();
    application.setId("app-1");
    when(jobApplicationRepository.findById("app-1")).thenReturn(Optional.of(application));
    when(notificationService.notifyAccepted(application, "Java Dev")).thenReturn(true);

    listener.handleJobApplicationEvent(event("APPLICATION_STATUS_CHANGED", "app-1", ApplicationStatus.ACCEPTED));

    verify(notificationService).notifyAccepted(application, "Java Dev");
  }

  @Test
  void handleJobApplicationEvent_shouldContinueWhenNotificationNotSent() {
    JobApplication application = new JobApplication();
    application.setId("app-1");
    when(jobApplicationRepository.findById("app-1")).thenReturn(Optional.of(application));
    when(notificationService.notifyAccepted(application, "Java Dev")).thenReturn(false);

    listener.handleJobApplicationEvent(event("APPLICATION_STATUS_CHANGED", "app-1", ApplicationStatus.ACCEPTED));

    verify(notificationService).notifyAccepted(application, "Java Dev");
  }

  private static JobApplicationEvent event(String type, String appId, ApplicationStatus status) {
    return new JobApplicationEvent(
        type,
        appId,
        "offer-1",
        "Java Dev",
        "Alice",
        "alice@example.com",
        status,
        Instant.now());
  }
}

