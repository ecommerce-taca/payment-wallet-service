package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.AdminTaxConfigView;
import com.taca.paymentwallet.application.port.out.AdminTaxConfigWriteModel;
import com.taca.paymentwallet.application.port.out.AdminTaxConfigWritePort;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.infrastructure.persistence.entity.TaxConfigJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.TaxConfigJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;

import java.time.Instant;
import java.util.Objects;

public class AdminTaxConfigWriteAdapter implements AdminTaxConfigWritePort {

    private final TaxConfigJpaRepository repository;
    private final PersistenceUuidGenerator uuidGenerator;
    private final ClockPort clockPort;

    public AdminTaxConfigWriteAdapter(
            TaxConfigJpaRepository repository,
            PersistenceUuidGenerator uuidGenerator,
            ClockPort clockPort
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.uuidGenerator = Objects.requireNonNull(uuidGenerator);
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    @Override
    public AdminTaxConfigView append(AdminTaxConfigWriteModel model) {
        Objects.requireNonNull(model, "model must not be null");

        Instant createdAt = clockPort.now();

        TaxConfigJpaEntity entity = new TaxConfigJpaEntity();
        entity.setId(uuidGenerator.next(createdAt));
        entity.setScope(model.scope());
        entity.setCategoryId(model.categoryId());
        entity.setRateBps(model.rateBps());
        entity.setEffectiveFrom(PersistenceTimeMapper.toLocalDateTime(model.effectiveFrom()));
        entity.setNote(model.note());
        entity.setCreatedBy(model.createdBy());
        entity.setCreatedAt(PersistenceTimeMapper.toLocalDateTime(createdAt));

        TaxConfigJpaEntity saved = repository.save(entity);

        return new AdminTaxConfigView(
                saved.getId(),
                saved.getScope(),
                saved.getCategoryId(),
                saved.getRateBps(),
                PersistenceTimeMapper.toInstant(saved.getEffectiveFrom()),
                saved.getNote(),
                saved.getCreatedBy(),
                PersistenceTimeMapper.toInstant(saved.getCreatedAt())
        );
    }
}