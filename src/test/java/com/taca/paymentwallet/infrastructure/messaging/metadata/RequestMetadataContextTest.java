package com.taca.paymentwallet.infrastructure.messaging.metadata;

import com.taca.paymentwallet.application.metadata.RequestMetadata;
import com.taca.paymentwallet.application.metadata.RequestMetadataContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestMetadataContextTest {

    @AfterEach
    void tearDown() {
        RequestMetadataContext.clear();
    }

    @Test
    void shouldStoreCurrentMetadata() {
        RequestMetadata metadata = new RequestMetadata(
                "req-001",
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                "vendor=value"
        );

        RequestMetadataContext.set(metadata);

        assertThat(RequestMetadataContext.current())
                .contains(metadata);
    }

    @Test
    void shouldClearMetadata() {
        RequestMetadataContext.set(
                new RequestMetadata(
                        "req-001",
                        null,
                        null
                )
        );

        RequestMetadataContext.clear();

        assertThat(RequestMetadataContext.current()).isEmpty();
    }

    @Test
    void shouldNotStoreEmptyMetadata() {
        RequestMetadataContext.set(
                new RequestMetadata(
                        null,
                        null,
                        null
                )
        );

        assertThat(RequestMetadataContext.current()).isEmpty();
    }
}