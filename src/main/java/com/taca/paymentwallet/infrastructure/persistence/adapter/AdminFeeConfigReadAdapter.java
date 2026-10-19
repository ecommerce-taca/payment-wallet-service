package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.AdminFeeConfigReadPort;
import com.taca.paymentwallet.application.port.out.AdminFeeConfigView;
import com.taca.paymentwallet.infrastructure.persistence.entity.FeeConfigJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.FeeConfigJpaRepository;

import java.util.List;
import java.util.Objects;

public class AdminFeeConfigReadAdapter implements AdminFeeConfigReadPort {

    private final FeeConfigJpaRepository repository;

    public AdminFeeConfigReadAdapter(FeeConfigJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public List<AdminFeeConfigView> findAll() {
        return repository
                .findAllByOrderByEffectiveFromDescCreatedAtDesc()
                .stream()
                .map(this::toView)
                .toList();
    }

    private AdminFeeConfigView toView(FeeConfigJpaEntity entity) {
        return new AdminFeeConfigView(
                entity.getId(),
                entity.getScope(),
                entity.getCategoryId(),
                entity.getRateBps(),
                PersistenceTimeMapper.toInstant(entity.getEffectiveFrom()),
                entity.getNote(),
                entity.getCreatedBy(),
                PersistenceTimeMapper.toInstant(entity.getCreatedAt())
        );
    }
}