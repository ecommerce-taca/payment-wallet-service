package com.taca.paymentwallet.infrastructure.export;

import com.taca.paymentwallet.application.export.AdminFinanceSummaryExportDownload;
import com.taca.paymentwallet.application.export.AdminFinanceSummaryExportFile;
import com.taca.paymentwallet.application.export.AdminFinanceSummaryExportLocation;
import com.taca.paymentwallet.application.port.out.ClockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TemporaryAdminFinanceSummaryExportStorageAdapterTest {

    private static final Instant NOW =
            Instant.parse("2026-10-23T16:00:00Z");

    private ClockPort clockPort;
    private TemporaryAdminFinanceSummaryExportStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        clockPort = mock(ClockPort.class);
        when(clockPort.now()).thenReturn(NOW);

        adapter =
                new TemporaryAdminFinanceSummaryExportStorageAdapter(
                        clockPort
                );
    }

    @Test
    void shouldStoreAndReadExport() {
        AdminFinanceSummaryExportFile file =
                new AdminFinanceSummaryExportFile(
                        "summary.csv",
                        "text/csv; charset=UTF-8",
                        "a,b\n1,2\n".getBytes(StandardCharsets.UTF_8)
                );

        AdminFinanceSummaryExportLocation location =
                adapter.store(
                        file,
                        Duration.ofMinutes(30)
                );

        assertThat(location.downloadUrl())
                .startsWith(
                        "/api/v1/admin/finance/summary/exports/"
                );

        assertThat(location.expiresAt())
                .isEqualTo(
                        NOW.plus(
                                Duration.ofMinutes(30)
                        )
                );

        String token =
                tokenFrom(
                        location.downloadUrl()
                );

        AdminFinanceSummaryExportDownload download =
                adapter.find(token)
                        .orElseThrow();

        assertThat(download.fileName())
                .isEqualTo("summary.csv");

        assertThat(download.contentType())
                .isEqualTo(
                        "text/csv; charset=UTF-8"
                );

        assertThat(
                new String(
                        download.content(),
                        StandardCharsets.UTF_8
                )
        ).isEqualTo(
                "a,b\n1,2\n"
        );
    }

    @Test
    void shouldExpireExportAfterTtl() {
        AdminFinanceSummaryExportLocation location =
                adapter.store(
                        file(),
                        Duration.ofMinutes(30)
                );

        String token =
                tokenFrom(
                        location.downloadUrl()
                );

        when(clockPort.now())
                .thenReturn(
                        NOW.plus(
                                Duration.ofMinutes(30)
                        )
                );

        assertThat(
                adapter.find(token)
        ).isEmpty();
    }

    @Test
    void shouldKeepExportBeforeExpiry() {
        AdminFinanceSummaryExportLocation location =
                adapter.store(
                        file(),
                        Duration.ofMinutes(30)
                );

        String token =
                tokenFrom(
                        location.downloadUrl()
                );

        when(clockPort.now())
                .thenReturn(
                        NOW.plusSeconds(
                                Duration.ofMinutes(30)
                                        .minusSeconds(1)
                                        .toSeconds()
                        )
                );

        assertThat(
                adapter.find(token)
        ).isPresent();
    }

    @Test
    void shouldReturnEmptyForUnknownToken() {
        assertThat(
                adapter.find(
                        "unknown-token"
                )
        ).isEmpty();
    }

    @Test
    void shouldReturnEmptyForBlankToken() {
        assertThat(
                adapter.find(" ")
        ).isEmpty();
    }

    @Test
    void shouldRejectZeroTtl() {
        assertThatThrownBy(() ->
                adapter.store(
                        file(),
                        Duration.ZERO
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "urlTtl must be positive"
                );
    }

    @Test
    void shouldRejectNegativeTtl() {
        assertThatThrownBy(() ->
                adapter.store(
                        file(),
                        Duration.ofMinutes(-1)
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "urlTtl must be positive"
                );
    }

    @Test
    void shouldGenerateDifferentTokens() {
        AdminFinanceSummaryExportLocation first =
                adapter.store(
                        file(),
                        Duration.ofMinutes(30)
                );

        AdminFinanceSummaryExportLocation second =
                adapter.store(
                        file(),
                        Duration.ofMinutes(30)
                );

        assertThat(
                tokenFrom(
                        first.downloadUrl()
                )
        ).isNotEqualTo(
                tokenFrom(
                        second.downloadUrl()
                )
        );
    }

    @Test
    void shouldProtectStoredContentFromMutation() {
        byte[] content =
                "original"
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        AdminFinanceSummaryExportLocation location =
                adapter.store(
                        new AdminFinanceSummaryExportFile(
                                "summary.csv",
                                "text/csv",
                                content
                        ),
                        Duration.ofMinutes(30)
                );

        content[0] = 'X';

        AdminFinanceSummaryExportDownload download =
                adapter.find(
                                tokenFrom(
                                        location.downloadUrl()
                                )
                        )
                        .orElseThrow();

        assertThat(
                new String(
                        download.content(),
                        StandardCharsets.UTF_8
                )
        ).isEqualTo(
                "original"
        );
    }

    private AdminFinanceSummaryExportFile file() {
        return new AdminFinanceSummaryExportFile(
                "summary.csv",
                "text/csv; charset=UTF-8",
                "a,b\n1,2\n"
                        .getBytes(
                                StandardCharsets.UTF_8
                        )
        );
    }

    private String tokenFrom(
            String downloadUrl
    ) {
        return downloadUrl.substring(
                downloadUrl.lastIndexOf('/') + 1
        );
    }
}