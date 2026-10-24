package com.taca.paymentwallet.application.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GetAdminPaymentReconciliationQueryTest {

    @Test
    void shouldAcceptValidPagination() {
        GetAdminPaymentReconciliationQuery query =
                new GetAdminPaymentReconciliationQuery(
                        1,
                        20
                );

        assertEquals(1, query.page());
        assertEquals(20, query.size());
    }

    @Test
    void shouldRejectPageBelowOne() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new GetAdminPaymentReconciliationQuery(
                        0,
                        20
                )
        );
    }

    @Test
    void shouldRejectOversizedPageSize() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new GetAdminPaymentReconciliationQuery(
                        1,
                        101
                )
        );
    }
}