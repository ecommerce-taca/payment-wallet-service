package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.*;
import com.taca.paymentwallet.infrastructure.persistence.entity.FeeConfigJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.FeeConfigJpaRepository;
import com.taca.paymentwallet.infrastructure.persistence.support.PersistenceUuidGenerator;

import java.time.Instant;
import java.util.Objects;

public class AdminFeeConfigWriteAdapter implements AdminFeeConfigWritePort {

    private final FeeConfigJpaRepository repository;
    private final PersistenceUuidGenerator uuidGenerator;
    private final ClockPort clockPort;

    public AdminFeeConfigWriteAdapter(
            FeeConfigJpaRepository repository,
            PersistenceUuidGenerator uuidGenerator,
            ClockPort clockPort
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.uuidGenerator = Objects.requireNonNull(uuidGenerator);
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    @Override
    public AdminFeeConfigView append(AdminFeeConfigWriteModel model) {
        Objects.requireNonNull(model, "model must not be null");

        Instant createdAt = clockPort.now();

        FeeConfigJpaEntity entity = new FeeConfigJpaEntity();
        entity.setId(uuidGenerator.next(createdAt));
        entity.setScope(model.scope());
        entity.setCategoryId(model.categoryId());
        entity.setRateBps(model.rateBps());
        entity.setEffectiveFrom(PersistenceTimeMapper.toLocalDateTime(model.effectiveFrom()));
        entity.setNote(model.note());
        entity.setCreatedBy(model.createdBy());
        entity.setCreatedAt(PersistenceTimeMapper.toLocalDateTime(createdAt));

        FeeConfigJpaEntity saved = repository.save(entity);

        return new AdminFeeConfigView(
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