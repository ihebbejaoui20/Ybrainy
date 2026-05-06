package com.ybrainy.joboffer.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ybrainy.joboffer.entity.ApplicationStatus;
import com.ybrainy.joboffer.entity.JobApplication;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class JobApplicationEventPublisherTest {

  @Test
  void publishCreated_shouldSendExpectedEvent() {
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    JobApplicationEventPublisher publisher =
        new JobApplicationEventPublisher(rabbitTemplate, "exchange.jobs", "application.key");

    JobApplication application = sampleApplication();

    publisher.publishCreated(application, "Java Developer");

    ArgumentCaptor<JobApplicationEvent> captor = ArgumentCaptor.forClass(JobApplicationEvent.class);
    verify(rabbitTemplate).convertAndSend(eq("exchange.jobs"), eq("application.key"), captor.capture());

    JobApplicationEvent event = captor.getValue();
    assertEquals("APPLICATION_CREATED", event.eventType());
    assertEquals("app-1", event.applicationId());
    assertEquals("offer-1", event.offerId());
    assertEquals("Java Developer", event.offerTitle());
    assertEquals("Alice", event.applicantName());
    assertEquals("alice@example.com", event.applicantEmail());
    assertEquals(ApplicationStatus.PENDING, event.status());
    assertNotNull(event.occurredAt());
  }

  @Test
  void publishStatusChanged_shouldSendExpectedEvent() {
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    JobApplicationEventPublisher publisher =
        new JobApplicationEventPublisher(rabbitTemplate, "exchange.jobs", "application.key");

    JobApplication application = sampleApplication();
    application.setStatus(ApplicationStatus.ACCEPTED);

    publisher.publishStatusChanged(application, "Java Developer");

    ArgumentCaptor<JobApplicationEvent> captor = ArgumentCaptor.forClass(JobApplicationEvent.class);
    verify(rabbitTemplate).convertAndSend(eq("exchange.jobs"), eq("application.key"), captor.capture());

    JobApplicationEvent event = captor.getValue();
    assertEquals("APPLICATION_STATUS_CHANGED", event.eventType());
    assertEquals(ApplicationStatus.ACCEPTED, event.status());
  }

  private static JobApplication sampleApplication() {
    JobApplication app = new JobApplication();
    app.setId("app-1");
    app.setOfferId("offer-1");
    app.setApplicantName("Alice");
    app.setApplicantEmail("alice@example.com");
    app.setStatus(ApplicationStatus.PENDING);
    return app;
  }
}

