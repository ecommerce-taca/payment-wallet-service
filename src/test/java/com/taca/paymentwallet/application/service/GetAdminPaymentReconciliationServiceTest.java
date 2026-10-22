package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.port.out.AdminPaymentReconciliationReadPort;
import com.taca.paymentwallet.application.port.out.AdminPaymentReconciliationView;
import com.taca.paymentwallet.application.query.GetAdminPaymentReconciliationQuery;
import com.taca.paymentwallet.application.result.GetAdminPaymentReconciliationResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GetAdminPaymentReconciliationServiceTest {

    @Test
    void shouldPaginateAndCountMismatches() {
        AdminPaymentReconciliationReadPort readPort =
                mock(AdminPaymentReconciliationReadPort.class);

        when(readPort.findAll()).thenReturn(
                List.of(
                        view(true),
                        view(false),
                        view(true)
                )
        );

        GetAdminPaymentReconciliationService service =
                new GetAdminPaymentReconciliationService(readPort);

        GetAdminPaymentReconciliationResult result =
                service.execute(
                        new GetAdminPaymentReconciliationQuery(
                                1,
                                2
                        )
                );

        assertThat(result.items()).hasSize(2);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.total()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.mismatchCount()).isEqualTo(1);

        verify(readPort).findAll();
    }

    @Test
    void shouldReturnSecondPage() {
        AdminPaymentReconciliationReadPort readPort =
                mock(AdminPaymentReconciliationReadPort.class);

        when(readPort.findAll()).thenReturn(
                List.of(
                        view(true),
                        view(true),
                        view(false)
                )
        );

        GetAdminPaymentReconciliationService service =
                new GetAdminPaymentReconciliationService(readPort);

        GetAdminPaymentReconciliationResult result =
                service.execute(
                        new GetAdminPaymentReconciliationQuery(
                                2,
                                2
                        )
                );

        assertThat(result.items()).hasSize(1);
        assertThat(result.total()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.mismatchCount()).isEqualTo(1);
    }

    @Test
    void shouldReturnEmptyPageWhenRequestedPageIsPastEnd() {
        AdminPaymentReconciliationReadPort readPort =
                mock(AdminPaymentReconciliationReadPort.class);

        when(readPort.findAll()).thenReturn(
                List.of(view(true))
        );

        GetAdminPaymentReconciliationService service =
                new GetAdminPaymentReconciliationService(readPort);

        GetAdminPaymentReconciliationResult result =
                service.execute(
                        new GetAdminPaymentReconciliationQuery(
                                2,
                                20
                        )
                );

        assertThat(result.items()).isEmpty();
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void shouldReturnEmptyResult() {
        AdminPaymentReconciliationReadPort readPort =
                mock(AdminPaymentReconciliationReadPort.class);

        when(readPort.findAll()).thenReturn(List.of());

        GetAdminPaymentReconciliationService service =
                new GetAdminPaymentReconciliationService(readPort);

        GetAdminPaymentReconciliationResult result =
                service.execute(
                        new GetAdminPaymentReconciliationQuery(
                                1,
                                20
                        )
                );

        assertThat(result.items()).isEmpty();
        assertThat(result.total()).isZero();
        assertThat(result.totalPages()).isZero();
        assertThat(result.mismatchCount()).isZero();
    }

    private AdminPaymentReconciliationView view(
            boolean matched
    ) {
        return new AdminPaymentReconciliationView(
                UUID.randomUUID(),
                "VNPAY",
                "SUCCESS",
                "SUCCESS",
                1_094_000L,
                matched,
                Instant.parse("2026-10-22T02:00:00Z")
        );
    }
}