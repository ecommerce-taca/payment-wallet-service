package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.CreateAdminFeeConfigCommand;
import com.taca.paymentwallet.application.port.in.CreateAdminFeeConfigUseCase;
import com.taca.paymentwallet.application.port.out.*;
import com.taca.paymentwallet.application.result.CreateAdminFeeConfigResult;

import java.time.Instant;
import java.util.Objects;

public class CreateAdminFeeConfigService implements CreateAdminFeeConfigUseCase {

    private final AdminFeeConfigWritePort writePort;
    private final AdminAuditPort auditPort;
    private final TransactionPort transactionPort;
    private final ClockPort clockPort;

    public CreateAdminFeeConfigService(
            AdminFeeConfigWritePort writePort,
            AdminAuditPort auditPort,
            TransactionPort transactionPort,
            ClockPort clockPort
    ) {
        this.writePort = Objects.requireNonNull(writePort);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.transactionPort = Objects.requireNonNull(transactionPort);
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    @Override
    public CreateAdminFeeConfigResult execute(CreateAdminFeeConfigCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return transactionPort.execute(() -> {
            AdminFeeConfigView created = writePort.append(
                    new AdminFeeConfigWriteModel(
                            command.scope(),
                            command.categoryId(),
                            command.rateBps(),
                            command.effectiveFrom(),
                            command.note(),
                            command.actorUserId()
                    )
            );

            Instant occurredAt = clockPort.now();

            auditPort.append(
                    command.actorUserId(),
                    "UPDATE_FEE_CONFIG",
                    "FEE_CONFIG",
                    created.id(),
                    command.note(),
                    occurredAt
            );

            return new CreateAdminFeeConfigResult(
                    created.id(),
                    created.scope(),
                    created.categoryId(),
                    created.rateBps(),
                    created.effectiveFrom(),
                    created.note(),
                    created.createdBy(),
                    created.createdAt()
            );
        });
    }
}