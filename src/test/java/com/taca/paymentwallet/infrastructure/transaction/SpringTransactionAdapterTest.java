package com.taca.paymentwallet.infrastructure.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SpringTransactionAdapterTest {

    private PlatformTransactionManager transactionManager;

    private SpringTransactionAdapter adapter;

    @BeforeEach
    void setUp() {
        transactionManager =
                mock(
                        PlatformTransactionManager.class
                );

        when(
                transactionManager.getTransaction(
                        any(TransactionDefinition.class)
                )
        ).thenReturn(
                new SimpleTransactionStatus()
        );

        adapter =
                new SpringTransactionAdapter(
                        transactionManager
                );
    }

    @Test
    void shouldReturnActionResultAndCommitTransaction() {
        String result =
                adapter.execute(
                        () -> "SUCCESS"
                );

        assertEquals(
                "SUCCESS",
                result
        );

        verify(transactionManager)
                .getTransaction(
                        any(TransactionDefinition.class)
                );

        verify(transactionManager)
                .commit(
                        any(TransactionStatus.class)
                );

        verify(transactionManager, never())
                .rollback(
                        any(TransactionStatus.class)
                );
    }

    @Test
    void shouldRollbackWhenActionThrowsRuntimeException() {
        RuntimeException failure =
                new RuntimeException(
                        "boom"
                );

        RuntimeException thrown =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                adapter.execute(
                                        () -> {
                                            throw failure;
                                        }
                                )
                );

        assertSame(
                failure,
                thrown
        );

        verify(transactionManager)
                .getTransaction(
                        any(TransactionDefinition.class)
                );

        verify(transactionManager)
                .rollback(
                        any(TransactionStatus.class)
                );

        verify(transactionManager, never())
                .commit(
                        any(TransactionStatus.class)
                );
    }

    @Test
    void shouldSupportVoidAction() {
        Runnable action =
                mock(Runnable.class);

        adapter.execute(action);

        verify(action)
                .run();

        verify(transactionManager)
                .commit(
                        any(TransactionStatus.class)
                );
    }

    @Test
    void shouldRejectNullSupplier() {
        assertThrows(
                NullPointerException.class,
                () ->
                        adapter.execute(
                                (java.util.function.Supplier<Object>) null
                        )
        );

        verifyNoInteractions(
                transactionManager
        );
    }
}