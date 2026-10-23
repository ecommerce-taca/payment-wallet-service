package com.taca.paymentwallet.application.export;

import com.taca.paymentwallet.application.port.out.AdminFinanceSummaryView;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AdminFinanceSummaryCsvRendererTest {

    private final AdminFinanceSummaryCsvRenderer renderer =
            new AdminFinanceSummaryCsvRenderer();

    @Test
    void shouldRenderFinanceSummaryAsCsv() {
        AdminFinanceSummaryView summary = summary();

        AdminFinanceSummaryExportFile file = renderer.render(summary);

        String csv = new String(file.content(), StandardCharsets.UTF_8);

        assertThat(file.contentType()).isEqualTo("text/csv; charset=UTF-8");
        assertThat(file.fileName())
                .isEqualTo("admin-finance-summary-2026-10-23T15-30-00Z.csv");

        assertThat(csv).isEqualTo("""
                captured_amount,refunded_amount,gross_amount,commission_amount,tax_amount,seller_net_amount,available_balance,pending_balance,completed_payout_amount,as_of
                1000000,100000,900000,70000,30000,800000,500000,300000,200000,2026-10-23T15:30:00Z
                """);
    }

    @Test
    void shouldRejectNullSummary() {
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> renderer.render(null)
        );
    }

    @Test
    void shouldProtectExportContentFromMutation() {
        byte[] content = "test".getBytes(StandardCharsets.UTF_8);

        AdminFinanceSummaryExportFile file =
                new AdminFinanceSummaryExportFile(
                        "test.csv",
                        "text/csv",
                        content
                );

        content[0] = 'X';

        assertThat(new String(file.content(), StandardCharsets.UTF_8))
                .isEqualTo("test");

        byte[] returned = file.content();
        returned[0] = 'Y';

        assertThat(new String(file.content(), StandardCharsets.UTF_8))
                .isEqualTo("test");
    }

    private AdminFinanceSummaryView summary() {
        return new AdminFinanceSummaryView(
                1_000_000,
                100_000,
                900_000,
                70_000,
                30_000,
                800_000,
                500_000,
                300_000,
                200_000,
                Instant.parse("2026-10-23T15:30:00Z")
        );
    }
}