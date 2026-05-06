package com.ybrainy.joboffer.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ybrainy.joboffer.entity.JobOffer;
import com.ybrainy.joboffer.entity.OfferStatus;
import com.ybrainy.joboffer.repository.JobOfferRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartnershipEventListenerTest {

  @Mock private JobOfferRepository jobOfferRepository;

  private PartnershipEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new PartnershipEventListener(jobOfferRepository);
  }

  @Test
  void handlePartnershipEvent_shouldIgnoreInvalidEvents() {
    listener.handlePartnershipEvent(null);
    listener.handlePartnershipEvent(new PartnershipEvent("PARTNERSHIP_DELETED", "   ", "n", "e", false, Instant.now()));

    verifyNoInteractions(jobOfferRepository);
  }

  @Test
  void handlePartnershipEvent_shouldIgnoreNonClosingEvents() {
    PartnershipEvent event = new PartnershipEvent("PARTNERSHIP_UPDATED", "p-1", "name", "mail", true, Instant.now());

    listener.handlePartnershipEvent(event);

    verifyNoInteractions(jobOfferRepository);
  }

  @Test
  void handlePartnershipEvent_shouldHandleNoOffers() {
    PartnershipEvent event = new PartnershipEvent("PARTNERSHIP_DELETED", "p-1", "name", "mail", false, Instant.now());
    when(jobOfferRepository.findAllByPartnershipId("p-1")).thenReturn(List.of());

    listener.handlePartnershipEvent(event);

    verify(jobOfferRepository).findAllByPartnershipId("p-1");
    verify(jobOfferRepository, never()).saveAll(anyList());
  }

  @Test
  void handlePartnershipEvent_shouldCloseNonClosedOffers() {
    PartnershipEvent event = new PartnershipEvent("PARTNERSHIP_DELETED", "p-1", "name", "mail", false, Instant.now());
    JobOffer open = new JobOffer();
    open.setStatus(OfferStatus.OPEN);
    JobOffer draft = new JobOffer();
    draft.setStatus(OfferStatus.DRAFT);
    JobOffer alreadyClosed = new JobOffer();
    alreadyClosed.setStatus(OfferStatus.CLOSED);

    when(jobOfferRepository.findAllByPartnershipId("p-1")).thenReturn(List.of(open, draft, alreadyClosed));

    listener.handlePartnershipEvent(event);

    ArgumentCaptor<List<JobOffer>> captor = ArgumentCaptor.forClass(List.class);
    verify(jobOfferRepository).saveAll(captor.capture());
    List<JobOffer> saved = captor.getValue();
    assertEquals(3, saved.size());
    assertEquals(OfferStatus.CLOSED, open.getStatus());
    assertEquals(OfferStatus.CLOSED, draft.getStatus());
    assertEquals(OfferStatus.CLOSED, alreadyClosed.getStatus());
  }
}

