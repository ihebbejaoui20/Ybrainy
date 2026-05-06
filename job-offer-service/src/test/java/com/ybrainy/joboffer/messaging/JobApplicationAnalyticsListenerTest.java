package com.ybrainy.joboffer.messaging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.ybrainy.joboffer.entity.ApplicationStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JobApplicationAnalyticsListenerTest {

  @Test
  void handle_shouldAcceptNullEvent() {
    JobApplicationAnalyticsListener listener = new JobApplicationAnalyticsListener();

    assertDoesNotThrow(() -> listener.handle(null));
  }

  @Test
  void handle_shouldLogValidEventWithoutFailure() {
    JobApplicationAnalyticsListener listener = new JobApplicationAnalyticsListener();
    JobApplicationEvent event =
        new JobApplicationEvent(
            "APPLICATION_STATUS_CHANGED",
            "app-1",
            "offer-1",
            "Java Dev",
            "Alice",
            "alice@example.com",
            ApplicationStatus.ACCEPTED,
            Instant.now());

    assertDoesNotThrow(() -> listener.handle(event));
  }
}

