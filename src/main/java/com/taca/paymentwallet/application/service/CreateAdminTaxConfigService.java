package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.CreateAdminTaxConfigCommand;
import com.taca.paymentwallet.application.port.in.CreateAdminTaxConfigUseCase;
import com.taca.paymentwallet.application.port.out.*;
import com.taca.paymentwallet.application.result.CreateAdminTaxConfigResult;

import java.time.Instant;
import java.util.Objects;

public class CreateAdminTaxConfigService implements CreateAdminTaxConfigUseCase {

    private final AdminTaxConfigWritePort writePort;
    private final AdminAuditPort auditPort;
    private final TransactionPort transactionPort;
    private final ClockPort clockPort;

    public CreateAdminTaxConfigService(
            AdminTaxConfigWritePort writePort,
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
    public CreateAdminTaxConfigResult execute(CreateAdminTaxConfigCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return transactionPort.execute(() -> {
            AdminTaxConfigView created = writePort.append(
                    new AdminTaxConfigWriteModel(
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
                    "UPDATE_TAX_CONFIG",
                    "TAX_CONFIG",
                    created.id(),
                    command.note(),
                    occurredAt
            );

            return new CreateAdminTaxConfigResult(
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