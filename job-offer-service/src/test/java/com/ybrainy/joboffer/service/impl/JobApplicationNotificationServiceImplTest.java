package com.ybrainy.joboffer.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ybrainy.joboffer.entity.JobApplication;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class JobApplicationNotificationServiceImplTest {

  @Mock private ObjectProvider<JavaMailSender> mailSenderProvider;
  @Mock private JavaMailSender mailSender;

  @Test
  void notifyAccepted_shouldReturnFalseWhenDisabled() {
    JobApplicationNotificationServiceImpl service =
        new JobApplicationNotificationServiceImpl(mailSenderProvider, "from@test.com", "smtp@test.com", false, "smtp-host");

    boolean sent = service.notifyAccepted(application("app-1", "Alice", "alice@example.com"), "Java Dev");

    assertFalse(sent);
    verifyNoInteractions(mailSenderProvider, mailSender);
  }

  @Test
  void notifyAccepted_shouldReturnFalseWhenMailHostMissing() {
    JobApplicationNotificationServiceImpl service =
        new JobApplicationNotificationServiceImpl(mailSenderProvider, "from@test.com", "smtp@test.com", true, "   ");

    boolean sent = service.notifyAccepted(application("app-1", "Alice", "alice@example.com"), "Java Dev");

    assertFalse(sent);
    verifyNoInteractions(mailSenderProvider, mailSender);
  }

  @Test
  void notifyAccepted_shouldReturnFalseWhenMailSenderUnavailable() {
    when(mailSenderProvider.getIfAvailable()).thenReturn(null);
    JobApplicationNotificationServiceImpl service =
        new JobApplicationNotificationServiceImpl(mailSenderProvider, "from@test.com", "smtp@test.com", true, "smtp-host");

    boolean sent = service.notifyAccepted(application("app-1", "Alice", "alice@example.com"), "Java Dev");

    assertFalse(sent);
    verify(mailSenderProvider).getIfAvailable();
    verifyNoInteractions(mailSender);
  }

  @Test
  void notifyAccepted_shouldReturnFalseWhenApplicantEmailMissing() {
    when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
    JobApplicationNotificationServiceImpl service =
        new JobApplicationNotificationServiceImpl(mailSenderProvider, "from@test.com", "smtp@test.com", true, "smtp-host");

    boolean sent = service.notifyAccepted(application("app-1", "Alice", "   "), "Java Dev");

    assertFalse(sent);
    verify(mailSenderProvider).getIfAvailable();
    verifyNoMoreInteractions(mailSender);
  }

  @Test
  void notifyAccepted_shouldSendHtmlMessageWhenPossible() throws Exception {
    MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

    JobApplicationNotificationServiceImpl service =
        new JobApplicationNotificationServiceImpl(mailSenderProvider, "jobs@company.com", "smtp@company.com", true, "smtp-host");

    boolean sent = service.notifyAccepted(application("app-1", "Alice", "alice@example.com"), "Java Dev");

    assertTrue(sent);
    verify(mailSender).send(mimeMessage);

    InternetAddress from = (InternetAddress) mimeMessage.getFrom()[0];
    assertEquals("jobs@company.com", from.getAddress());
    assertEquals("Application Accepted - Java Dev", mimeMessage.getSubject());
  }

  @Test
  void notifyAccepted_shouldFallbackToPlainTextWhenHtmlFails() {
    MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    doThrow(new RuntimeException("html fail")).when(mailSender).send(any(MimeMessage.class));

    JobApplicationNotificationServiceImpl service =
        new JobApplicationNotificationServiceImpl(
            mailSenderProvider, "no-reply@ybrainy.local", "smtp-user@example.com", true, "smtp-host");

    boolean sent = service.notifyAccepted(application("app-1", "   ", "alice@example.com"), "   ");

    assertTrue(sent);
    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());
    SimpleMailMessage fallback = captor.getValue();
    assertEquals("smtp-user@example.com", fallback.getFrom());
    assertArrayEquals(new String[] {"alice@example.com"}, fallback.getTo());
    assertEquals("Application Accepted - Offre d'emploi", fallback.getSubject());
    assertTrue(fallback.getText().contains("Hello Candidate"));
  }

  @Test
  void notifyAccepted_shouldUseDefaultFromWhenConfiguredAndUsernameBlank() {
    MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    doThrow(new RuntimeException("html fail")).when(mailSender).send(any(MimeMessage.class));

    JobApplicationNotificationServiceImpl service =
        new JobApplicationNotificationServiceImpl(mailSenderProvider, "   ", "   ", true, "smtp-host");

    boolean sent = service.notifyAccepted(application("app-1", "Alice", "alice@example.com"), "Java Dev");

    assertTrue(sent);
    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());
    assertEquals("no-reply@ybrainy.local", captor.getValue().getFrom());
  }

  @Test
  void notifyAccepted_shouldReturnFalseWhenBothHtmlAndFallbackFail() {
    MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    doThrow(new RuntimeException("html fail")).when(mailSender).send(any(MimeMessage.class));
    doThrow(new RuntimeException("plain fail")).when(mailSender).send(any(SimpleMailMessage.class));

    JobApplicationNotificationServiceImpl service =
        new JobApplicationNotificationServiceImpl(mailSenderProvider, "from@test.com", "smtp@test.com", true, "smtp-host");

    boolean sent = service.notifyAccepted(application("app-1", "Alice", "alice@example.com"), "Java Dev");

    assertFalse(sent);
  }

  private static JobApplication application(String id, String name, String email) {
    JobApplication app = new JobApplication();
    app.setId(id);
    app.setApplicantName(name);
    app.setApplicantEmail(email);
    return app;
  }
}

