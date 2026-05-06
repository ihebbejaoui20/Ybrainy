package com.ybrainy.joboffer.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ybrainy.joboffer.dto.JobApplicationRequest;
import com.ybrainy.joboffer.dto.JobApplicationResponse;
import com.ybrainy.joboffer.dto.JobApplicationUpdateRequest;
import com.ybrainy.joboffer.entity.ApplicationStatus;
import com.ybrainy.joboffer.entity.JobApplication;
import com.ybrainy.joboffer.entity.JobOffer;
import com.ybrainy.joboffer.exception.BusinessException;
import com.ybrainy.joboffer.exception.ResourceNotFoundException;
import com.ybrainy.joboffer.mapper.JobApplicationMapper;
import com.ybrainy.joboffer.messaging.JobApplicationEventPublisher;
import com.ybrainy.joboffer.repository.JobApplicationRepository;
import com.ybrainy.joboffer.repository.JobOfferRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceImplTest {

    @Mock private JobApplicationRepository applicationRepository;
    @Mock private JobOfferRepository offerRepository;
    @Mock private JobApplicationMapper mapper;
    @Mock private JobApplicationEventPublisher eventPublisher;

    private JobApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new JobApplicationServiceImpl(applicationRepository, offerRepository, mapper, eventPublisher);
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_whenOfferNotFound_throwsResourceNotFoundException() {
        when(offerRepository.findById("offer-1")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.create("offer-1", applicationRequest("alice@example.com")));

        assertTrue(ex.getMessage().contains("Job offer not found"));
        verifyNoInteractions(applicationRepository, mapper, eventPublisher);
    }

    @Test
    void create_whenDuplicateEmail_throwsBusinessException() {
        JobOffer offer = jobOffer("offer-1", "Dev Java");
        when(offerRepository.findById("offer-1")).thenReturn(Optional.of(offer));
        when(applicationRepository.existsByOfferIdAndApplicantEmailIgnoreCase("offer-1", "alice@example.com"))
                .thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create("offer-1", applicationRequest("alice@example.com")));

        assertTrue(ex.getMessage().contains("already applied"));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void create_withValidRequest_savesAndPublishesEvent() {
        JobOffer offer = jobOffer("offer-1", "Dev Java");
        JobApplicationRequest request = applicationRequest("alice@example.com");
        JobApplication entity = applicationEntity("app-1", "offer-1", ApplicationStatus.PENDING);
        JobApplicationResponse response = applicationResponse("app-1");

        when(offerRepository.findById("offer-1")).thenReturn(Optional.of(offer));
        when(applicationRepository.existsByOfferIdAndApplicantEmailIgnoreCase("offer-1", "alice@example.com"))
                .thenReturn(false);
        when(mapper.toEntity("offer-1", request)).thenReturn(entity);
        when(applicationRepository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);

        JobApplicationResponse result = service.create("offer-1", request);

        assertEquals(response, result);
        verify(applicationRepository).save(entity);
        verify(eventPublisher).publishCreated(entity, "Dev Java");
    }

    @Test
    void create_withOfferIdWithSpaces_trimsBeforeSearch() {
        when(offerRepository.findById("offer-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.create("  offer-1  ", applicationRequest("a@b.com")));

        verify(offerRepository).findById("offer-1");
    }

    @Test
    void create_withNullOfferTitle_usesDefaultTitle() {
        JobOffer offer = jobOffer("offer-1", null); // title null → resolveOfferTitle → "Offre d'emploi"
        JobApplicationRequest request = applicationRequest("bob@example.com");
        JobApplication entity = applicationEntity("app-2", "offer-1", ApplicationStatus.PENDING);

        when(offerRepository.findById("offer-1")).thenReturn(Optional.of(offer));
        when(applicationRepository.existsByOfferIdAndApplicantEmailIgnoreCase("offer-1", "bob@example.com"))
                .thenReturn(false);
        when(mapper.toEntity("offer-1", request)).thenReturn(entity);
        when(applicationRepository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(applicationResponse("app-2"));

        service.create("offer-1", request);

        verify(eventPublisher).publishCreated(entity, "Offre d'emploi");
    }

    @Test
    void create_withBlankOfferTitle_usesDefaultTitle() {
        JobOffer offer = jobOffer("offer-1", "   ");
        JobApplicationRequest request = applicationRequest("carol@example.com");
        JobApplication entity = applicationEntity("app-3", "offer-1", ApplicationStatus.PENDING);

        when(offerRepository.findById("offer-1")).thenReturn(Optional.of(offer));
        when(applicationRepository.existsByOfferIdAndApplicantEmailIgnoreCase("offer-1", "carol@example.com"))
                .thenReturn(false);
        when(mapper.toEntity("offer-1", request)).thenReturn(entity);
        when(applicationRepository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(applicationResponse("app-3"));

        service.create("offer-1", request);

        verify(eventPublisher).publishCreated(entity, "Offre d'emploi");
    }

    // ── listByOffer ───────────────────────────────────────────────────────────

    @Test
    void listByOffer_whenOfferNotFound_throwsResourceNotFoundException() {
        when(offerRepository.existsById("missing")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.listByOffer("missing"));

        verify(applicationRepository, never()).findByOfferIdOrderByCreatedAtDesc(any());
    }

    @Test
    void listByOffer_whenOfferExists_returnsApplications() {
        when(offerRepository.existsById("offer-1")).thenReturn(true);
        JobApplication app = applicationEntity("app-1", "offer-1", ApplicationStatus.PENDING);
        JobApplicationResponse response = applicationResponse("app-1");

        when(applicationRepository.findByOfferIdOrderByCreatedAtDesc("offer-1")).thenReturn(List.of(app));
        when(mapper.toResponse(app)).thenReturn(response);

        List<JobApplicationResponse> results = service.listByOffer("offer-1");

        assertEquals(1, results.size());
        assertEquals(response, results.get(0));
    }

    @Test
    void listByOffer_trimsOfferId() {
        when(offerRepository.existsById("offer-1")).thenReturn(true);
        when(applicationRepository.findByOfferIdOrderByCreatedAtDesc("offer-1")).thenReturn(List.of());

        service.listByOffer("  offer-1  ");

        verify(offerRepository).existsById("offer-1");
        verify(applicationRepository).findByOfferIdOrderByCreatedAtDesc("offer-1");
    }

    // ── listAll ───────────────────────────────────────────────────────────────

    @Test
    void listAll_returnsAllApplicationsOrderedByDate() {
        JobApplication app1 = applicationEntity("app-1", "offer-1", ApplicationStatus.PENDING);
        JobApplication app2 = applicationEntity("app-2", "offer-2", ApplicationStatus.ACCEPTED);
        JobApplicationResponse r1 = applicationResponse("app-1");
        JobApplicationResponse r2 = applicationResponse("app-2");

        when(applicationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(app1, app2));
        when(mapper.toResponse(app1)).thenReturn(r1);
        when(mapper.toResponse(app2)).thenReturn(r2);

        List<JobApplicationResponse> results = service.listAll();

        assertEquals(2, results.size());
    }

    @Test
    void listAll_whenNoApplications_returnsEmptyList() {
        when(applicationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<JobApplicationResponse> results = service.listAll();

        assertTrue(results.isEmpty());
    }

    // ── updateReview ──────────────────────────────────────────────────────────

    @Test
    void updateReview_whenApplicationNotFound_throwsResourceNotFoundException() {
        when(applicationRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateReview("missing", updateRequest(ApplicationStatus.REVIEWED, null)));

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void updateReview_whenStatusChanges_publishesStatusChangedEvent() {
        JobApplication app = applicationEntity("app-1", "offer-1", ApplicationStatus.PENDING);
        JobApplicationUpdateRequest updateRequest = updateRequest(ApplicationStatus.REVIEWED, "Good profile");
        JobApplicationResponse response = applicationResponse("app-1");
        JobOffer offer = jobOffer("offer-1", "Dev Java");

        when(applicationRepository.findById("app-1")).thenReturn(Optional.of(app));
        doAnswer(inv -> {
            JobApplication a = inv.getArgument(0);
            JobApplicationUpdateRequest r = inv.getArgument(1);
            a.setStatus(r.status());
            a.setReviewerNotes(r.reviewerNotes());
            return null;
        }).when(mapper).applyReview(app, updateRequest);
        when(applicationRepository.save(app)).thenReturn(app);
        when(offerRepository.findById("offer-1")).thenReturn(Optional.of(offer));
        when(mapper.toResponse(app)).thenReturn(response);

        service.updateReview("app-1", updateRequest);

        verify(eventPublisher).publishStatusChanged(app, "Dev Java");
    }

    @Test
    void updateReview_whenStatusDoesNotChange_doesNotPublishEvent() {
        JobApplication app = applicationEntity("app-1", "offer-1", ApplicationStatus.PENDING);
        JobApplicationUpdateRequest updateRequest = updateRequest(ApplicationStatus.PENDING, "Notes");
        JobApplicationResponse response = applicationResponse("app-1");

        when(applicationRepository.findById("app-1")).thenReturn(Optional.of(app));
        // applyReview does NOT change status (stays PENDING)
        doNothing().when(mapper).applyReview(app, updateRequest);
        when(applicationRepository.save(app)).thenReturn(app);
        when(mapper.toResponse(app)).thenReturn(response);

        service.updateReview("app-1", updateRequest);

        verify(eventPublisher, never()).publishStatusChanged(any(), any());
    }

    @Test
    void updateReview_whenStatusChangedToAccepted_logsAndPublishesEvent() {
        JobApplication app = applicationEntity("app-1", "offer-1", ApplicationStatus.PENDING);
        JobApplicationUpdateRequest updateRequest = updateRequest(ApplicationStatus.ACCEPTED, null);
        JobApplicationResponse response = applicationResponse("app-1");
        JobOffer offer = jobOffer("offer-1", "Senior Dev");

        when(applicationRepository.findById("app-1")).thenReturn(Optional.of(app));
        doAnswer(inv -> {
            ((JobApplication) inv.getArgument(0)).setStatus(ApplicationStatus.ACCEPTED);
            return null;
        }).when(mapper).applyReview(app, updateRequest);
        when(applicationRepository.save(app)).thenReturn(app);
        when(offerRepository.findById("offer-1")).thenReturn(Optional.of(offer));
        when(mapper.toResponse(app)).thenReturn(response);

        JobApplicationResponse result = service.updateReview("app-1", updateRequest);

        assertNotNull(result);
        verify(eventPublisher).publishStatusChanged(app, "Senior Dev");
    }

    @Test
    void updateReview_whenOfferNotFoundForTitle_usesDefaultTitle() {
        JobApplication app = applicationEntity("app-1", "offer-1", ApplicationStatus.PENDING);
        JobApplicationUpdateRequest updateRequest = updateRequest(ApplicationStatus.REJECTED, null);

        when(applicationRepository.findById("app-1")).thenReturn(Optional.of(app));
        doAnswer(inv -> {
            ((JobApplication) inv.getArgument(0)).setStatus(ApplicationStatus.REJECTED);
            return null;
        }).when(mapper).applyReview(app, updateRequest);
        when(applicationRepository.save(app)).thenReturn(app);
        when(offerRepository.findById("offer-1")).thenReturn(Optional.empty());
        when(mapper.toResponse(app)).thenReturn(applicationResponse("app-1"));

        service.updateReview("app-1", updateRequest);

        verify(eventPublisher).publishStatusChanged(app, "Offre d'emploi");
    }

    @Test
    void updateReview_trimsApplicationId() {
        when(applicationRepository.findById("app-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateReview("  app-1  ", updateRequest(ApplicationStatus.REVIEWED, null)));

        verify(applicationRepository).findById("app-1");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_whenApplicationExists_deletesIt() {
        JobApplication app = applicationEntity("app-1", "offer-1", ApplicationStatus.PENDING);
        when(applicationRepository.findById("app-1")).thenReturn(Optional.of(app));

        service.delete("app-1");

        verify(applicationRepository).delete(app);
    }

    @Test
    void delete_whenApplicationNotFound_throwsResourceNotFoundException() {
        when(applicationRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete("missing"));

        verify(applicationRepository, never()).delete(any());
    }

    @Test
    void delete_trimsApplicationId() {
        JobApplication app = applicationEntity("app-1", "offer-1", ApplicationStatus.PENDING);
        when(applicationRepository.findById("app-1")).thenReturn(Optional.of(app));

        service.delete("  app-1  ");

        verify(applicationRepository).delete(app);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static JobApplicationRequest applicationRequest(String email) {
        return new JobApplicationRequest("Alice", email, "Motivated", null);
    }

    private static JobApplicationUpdateRequest updateRequest(ApplicationStatus status, String notes) {
        return new JobApplicationUpdateRequest(status, notes);
    }

    private static JobApplication applicationEntity(String id, String offerId, ApplicationStatus status) {
        JobApplication app = new JobApplication();
        app.setId(id);
        app.setOfferId(offerId);
        app.setApplicantName("Alice");
        app.setApplicantEmail("alice@example.com");
        app.setStatus(status);
        app.setCreatedAt(Instant.now());
        return app;
    }

    private static JobApplicationResponse applicationResponse(String id) {
        return new JobApplicationResponse(
                id, "offer-1", "Alice", "alice@example.com",
                "Motivated", null, ApplicationStatus.PENDING, null, Instant.now());
    }

    private static JobOffer jobOffer(String id, String title) {
        JobOffer offer = new JobOffer();
        offer.setId(id);
        offer.setTitle(title);
        return offer;
    }
}