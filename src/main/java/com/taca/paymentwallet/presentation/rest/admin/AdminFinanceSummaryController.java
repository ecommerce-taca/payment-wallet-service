package com.taca.paymentwallet.presentation.rest.admin;

import com.taca.paymentwallet.application.export.AdminFinanceSummaryExportDownload;
import com.taca.paymentwallet.application.port.in.DownloadAdminFinanceSummaryExportUseCase;
import com.taca.paymentwallet.application.port.in.ExportAdminFinanceSummaryUseCase;
import com.taca.paymentwallet.application.port.in.GetAdminFinanceSummaryUseCase;
import com.taca.paymentwallet.application.result.ExportAdminFinanceSummaryResult;
import com.taca.paymentwallet.application.result.GetAdminFinanceSummaryResult;
import com.taca.paymentwallet.application.security.FinanceOpsPolicy;
import com.taca.paymentwallet.presentation.rest.ApiMeta;
import com.taca.paymentwallet.presentation.rest.ApiResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/admin/finance")
public class AdminFinanceSummaryController {

    private final GetAdminFinanceSummaryUseCase useCase;
    private final ExportAdminFinanceSummaryUseCase exportUseCase;
    private final DownloadAdminFinanceSummaryExportUseCase downloadUseCase;
    private final FinanceOpsPolicy financeOpsPolicy;

    public AdminFinanceSummaryController(
            GetAdminFinanceSummaryUseCase useCase,
            ExportAdminFinanceSummaryUseCase exportUseCase,
            DownloadAdminFinanceSummaryExportUseCase downloadUseCase,
            FinanceOpsPolicy financeOpsPolicy
    ) {
        this.useCase = Objects.requireNonNull(useCase);
        this.exportUseCase = Objects.requireNonNull(exportUseCase);
        this.downloadUseCase = Objects.requireNonNull(downloadUseCase);
        this.financeOpsPolicy = Objects.requireNonNull(financeOpsPolicy);
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminFinanceSummaryData>> getSummary(
            @RequestHeader("X-Request-ID") String requestId
    ) {
        financeOpsPolicy.requireFinanceOps();

        return ResponseEntity.ok(new ApiResponse<>(
                toData(useCase.execute()),
                new ApiMeta(requestId)
        ));
    }

    @GetMapping("/summary/export")
    public ResponseEntity<ApiResponse<AdminFinanceSummaryExportData>> exportSummary(
            @RequestHeader("X-Request-ID") String requestId
    ) {
        financeOpsPolicy.requireFinanceOps();

        ExportAdminFinanceSummaryResult result = exportUseCase.execute();

        return ResponseEntity.ok(new ApiResponse<>(
                new AdminFinanceSummaryExportData(
                        result.downloadUrl(),
                        result.expiresAt()
                ),
                new ApiMeta(requestId)
        ));
    }

    @GetMapping("/summary/exports/{token}")
    public ResponseEntity<byte[]> downloadExport(
            @PathVariable String token
    ) {
        AdminFinanceSummaryExportDownload export =
                downloadUseCase.execute(token);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(export.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(export.contentType()))
                .body(export.content());
    }

    private AdminFinanceSummaryData toData(
            GetAdminFinanceSummaryResult result
    ) {
        return new AdminFinanceSummaryData(
                result.capturedAmount(),
                result.refundedAmount(),
                result.grossAmount(),
                result.commissionAmount(),
                result.taxAmount(),
                result.sellerNetAmount(),
                result.availableBalance(),
                result.pendingBalance(),
                result.completedPayoutAmount(),
                result.asOf()
        );
    }
}