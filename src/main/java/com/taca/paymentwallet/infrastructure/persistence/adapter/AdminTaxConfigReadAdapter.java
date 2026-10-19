package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.AdminTaxConfigReadPort;
import com.taca.paymentwallet.application.port.out.AdminTaxConfigView;
import com.taca.paymentwallet.infrastructure.persistence.entity.TaxConfigJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.TaxConfigJpaRepository;

import java.util.List;
import java.util.Objects;

public class AdminTaxConfigReadAdapter implements AdminTaxConfigReadPort {

    private final TaxConfigJpaRepository repository;

    public AdminTaxConfigReadAdapter(TaxConfigJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public List<AdminTaxConfigView> findAll() {
        return repository
                .findAllByOrderByEffectiveFromDescCreatedAtDesc()
                .stream()
                .map(this::toView)
                .toList();
    }

    private AdminTaxConfigView toView(TaxConfigJpaEntity entity) {
        return new AdminTaxConfigView(
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