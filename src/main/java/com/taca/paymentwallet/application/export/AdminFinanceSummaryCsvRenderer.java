package com.taca.paymentwallet.application.export;

import com.taca.paymentwallet.application.port.out.AdminFinanceSummaryView;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class AdminFinanceSummaryCsvRenderer {

    private static final String HEADER =
            "captured_amount,refunded_amount,gross_amount,commission_amount,"
                    + "tax_amount,seller_net_amount,available_balance,"
                    + "pending_balance,completed_payout_amount,as_of";

    public AdminFinanceSummaryExportFile render(
            AdminFinanceSummaryView summary
    ) {
        Objects.requireNonNull(summary, "summary must not be null");

        String row = String.join(",",
                Long.toString(summary.capturedAmount()),
                Long.toString(summary.refundedAmount()),
                Long.toString(summary.grossAmount()),
                Long.toString(summary.commissionAmount()),
                Long.toString(summary.taxAmount()),
                Long.toString(summary.sellerNetAmount()),
                Long.toString(summary.availableBalance()),
                Long.toString(summary.pendingBalance()),
                Long.toString(summary.completedPayoutAmount()),
                DateTimeFormatter.ISO_INSTANT.format(summary.asOf())
        );

        String csv = HEADER + "\n" + row + "\n";

        return new AdminFinanceSummaryExportFile(
                fileName(summary),
                "text/csv; charset=UTF-8",
                csv.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String fileName(AdminFinanceSummaryView summary) {
        return "admin-finance-summary-"
                + DateTimeFormatter.ISO_INSTANT.format(summary.asOf())
                .replace(":", "-")
                + ".csv";
    }
}