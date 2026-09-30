package com.taca.paymentwallet.infrastructure.transaction;

import com.taca.paymentwallet.application.port.out.TransactionPort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.function.Supplier;

public class SpringTransactionAdapter
        implements TransactionPort {

    private final TransactionTemplate transactionTemplate;

    public SpringTransactionAdapter(
            PlatformTransactionManager transactionManager
    ) {
        Objects.requireNonNull(
                transactionManager,
                "transactionManager must not be null"
        );

        this.transactionTemplate =
                new TransactionTemplate(
                        transactionManager
                );
    }

    @Override
    public <T> T execute(
            Supplier<T> action
    ) {
        Objects.requireNonNull(
                action,
                "action must not be null"
        );

        return transactionTemplate.execute(
                status -> action.get()
        );
    }
}