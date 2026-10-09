package com.taca.paymentwallet.presentation.rest.payment;

import com.taca.paymentwallet.application.command.RequestRefundCommand;
import com.taca.paymentwallet.application.result.RequestRefundResult;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RefundRestMapperTest {

    private final RefundRestMapper mapper =
            new RefundRestMapper();

    @Test
    void shouldMapRefundRequestToCommand() {
        UUID paymentId = UUID.randomUUID();

        RequestRefundRequest request =
                new RequestRefundRequest(
                        50_000,
                        "Buyer requested refund"
                );

        RequestRefundCommand command =
                mapper.toCommand(
                        paymentId,
                        request,
                        "refund-idem-001"
                );

        assertEquals(
                paymentId,
                command.paymentId()
        );

        assertEquals(
                50_000L,
                command.amount()
        );

        assertEquals(
                "VND",
                command.currency()
        );

        assertEquals(
                "Buyer requested refund",
                command.reason()
        );

        assertEquals(
                "refund-idem-001",
                command.idempotencyKey()
        );
    }

    @Test
    void shouldMapRefundResultToData() {
        UUID refundId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        RequestRefundResult result =
                new RequestRefundResult(
                        refundId,
                        paymentId,
                        50_000,
                        "VND",
                        "REQUESTED"
                );

        RequestRefundData data =
                mapper.toData(result);

        assertEquals(
                refundId,
                data.refundId()
        );

        assertEquals(
                paymentId,
                data.paymentId()
        );

        assertEquals(
                50_000L,
                data.amount()
        );

        assertEquals(
                "VND",
                data.currency()
        );

        assertEquals(
                "REQUESTED",
                data.status()
        );
    }
}