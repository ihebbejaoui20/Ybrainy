package com.ybrainy.partnership.service.impl;

import com.ybrainy.partnership.entity.Partnership;
import com.ybrainy.partnership.dto.PartnershipRequest;
import com.ybrainy.partnership.dto.PartnershipResponse;
import com.ybrainy.partnership.exception.BusinessException;
import com.ybrainy.partnership.exception.ResourceNotFoundException;
import com.ybrainy.partnership.mapper.PartnershipMapper;
import com.ybrainy.partnership.repository.PartnershipRepository;
import com.ybrainy.partnership.service.PartnershipService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnershipServiceImpl implements PartnershipService {

  private final PartnershipRepository repository;
  private final PartnershipMapper mapper;

  public PartnershipServiceImpl(PartnershipRepository repository, PartnershipMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public PartnershipResponse create(PartnershipRequest request) {
    String email = request.email().trim().toLowerCase();
    if (repository.existsByEmailIgnoreCase(email)) {
      throw new BusinessException("A partnership already exists with this email");
    }
    Partnership saved = repository.save(mapper.toEntity(request));
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public PartnershipResponse update(String id, PartnershipRequest request) {
    Partnership entity = findOrThrow(id);
    String email = request.email().trim().toLowerCase();
    if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
      throw new BusinessException("Another partnership already uses this email");
    }
    mapper.apply(entity, request);
    return mapper.toResponse(repository.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public PartnershipResponse getById(String id) {
    return mapper.toResponse(findOrThrow(id));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PartnershipResponse> getAll(String search, Pageable pageable) {
    Page<Partnership> page =
        (search == null || search.isBlank())
            ? repository.findAll(pageable)
            : repository.findByNameContainingIgnoreCase(search.trim(), pageable);
    return page.map(mapper::toResponse);
  }

  @Override
  @Transactional
  public void delete(String id) {
    Partnership entity = findOrThrow(id);
    repository.delete(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsById(String id) {
    return repository.existsById(id);
  }

  private Partnership findOrThrow(String id) {
    return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Partnership not found: " + id));
  }
}
