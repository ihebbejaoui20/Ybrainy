package com.ybrainy.joboffer.service.impl;

import com.ybrainy.joboffer.client.PartnershipClient;
import com.ybrainy.joboffer.entity.JobOffer;
import com.ybrainy.joboffer.entity.OfferStatus;
import com.ybrainy.joboffer.dto.JobOfferRequest;
import com.ybrainy.joboffer.dto.JobOfferResponse;
import com.ybrainy.joboffer.exception.BusinessException;
import com.ybrainy.joboffer.exception.ResourceNotFoundException;
import com.ybrainy.joboffer.mapper.JobOfferMapper;
import com.ybrainy.joboffer.repository.JobOfferRepository;
import com.ybrainy.joboffer.service.JobOfferService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobOfferServiceImpl implements JobOfferService {

  private final JobOfferRepository repository;
  private final JobOfferMapper mapper;
  private final PartnershipClient partnershipClient;

  public JobOfferServiceImpl(JobOfferRepository repository, JobOfferMapper mapper, PartnershipClient partnershipClient) {
    this.repository = repository;
    this.mapper = mapper;
    this.partnershipClient = partnershipClient;
  }

  @Override
  @Transactional
  public JobOfferResponse create(JobOfferRequest request) {
    validatePartnership(request.partnershipId());
    validateSalaryRange(request);
    JobOffer saved = repository.save(mapper.toEntity(request));
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public JobOfferResponse update(String id, JobOfferRequest request) {
    validatePartnership(request.partnershipId());
    validateSalaryRange(request);
    JobOffer entity = findOrThrow(id);
    mapper.apply(entity, request);
    return mapper.toResponse(repository.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public JobOfferResponse getById(String id) {
    return mapper.toResponse(findOrThrow(id));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<JobOfferResponse> getAll(String partnershipId, OfferStatus status, String keyword, Pageable pageable) {
    Page<JobOffer> page;
    if (partnershipId != null && !partnershipId.isBlank()) {
      page = repository.findByPartnershipId(partnershipId.trim(), pageable);
    } else if (status != null) {
      page = repository.findByStatus(status, pageable);
    } else if (keyword != null && !keyword.isBlank()) {
      page = repository.findByTitleContainingIgnoreCase(keyword.trim(), pageable);
    } else {
      page = repository.findAll(pageable);
    }
    return page.map(mapper::toResponse);
  }

  @Override
  @Transactional
  public void delete(String id) {
    JobOffer entity = findOrThrow(id);
    repository.delete(entity);
  }

  private JobOffer findOrThrow(String id) {
    return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job offer not found: " + id));
  }

  private void validatePartnership(String partnershipId) {
    boolean exists;
    try {
      exists = partnershipClient.exists(partnershipId).exists();
    } catch (Exception ex) {
      throw new BusinessException("Unable to validate partnership. Check partnership-service availability.");
    }
    if (!exists) {
      throw new BusinessException("Invalid partnershipId: " + partnershipId);
    }
  }

  private void validateSalaryRange(JobOfferRequest request) {
    if (request.salaryMin() != null && request.salaryMax() != null && request.salaryMin().compareTo(request.salaryMax()) > 0) {
      throw new BusinessException("salaryMin cannot be greater than salaryMax");
    }
  }
}
