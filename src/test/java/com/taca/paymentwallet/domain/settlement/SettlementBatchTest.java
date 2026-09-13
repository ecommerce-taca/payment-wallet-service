package com.taca.paymentwallet.domain.settlement;

import com.taca.paymentwallet.domain.valueobject.LedgerPostingId;
import com.taca.paymentwallet.domain.valueobject.Money;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.SettlementBatchId;
import com.taca.paymentwallet.domain.valueobject.SettlementBatchItemId;
import com.taca.paymentwallet.domain.valueobject.SettlementLineId;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SettlementBatchTest {

    @Test
    void shouldCreatePendingSettlementBatch() {
        SettlementBatch batch = createBatch();

        assertEquals(SettlementBatchStatus.PENDING, batch.status());
        assertEquals(Money.vnd(92_000), batch.totalReleased());
    }

    @Test
    void shouldCompleteSettlementBatchWhenAllItemsCompleted() {
        SettlementBatchItem item = createItem(new PaymentAllocationId(UUID.randomUUID()));
        SettlementBatch batch = createBatchWithItem(item);

        batch.markProcessing();
        item.markCompleted(new LedgerPostingId(UUID.randomUUID()));
        batch.markCompleted();

        assertEquals(SettlementBatchStatus.COMPLETED, batch.status());
    }

    @Test
    void shouldRejectCompletedBatchWhenItemIsNotCompleted() {
        SettlementBatch batch = createBatch();

        batch.markProcessing();

        assertThrows(InvalidSettlementStateException.class, batch::markCompleted);
    }

    @Test
    void shouldRejectDuplicatePaymentAllocationInSameBatch() {
        PaymentAllocationId allocationId = new PaymentAllocationId(UUID.randomUUID());

        SettlementBatchItem item1 = createItem(allocationId);
        SettlementBatchItem item2 = createItem(allocationId);

        assertThrows(IllegalArgumentException.class,
                () -> createBatchWithItems(List.of(item1, item2)));
    }

    @Test
    void shouldRejectInvalidPeriod() {
        Instant now = Instant.now();

        assertThrows(IllegalArgumentException.class, () -> new SettlementBatch(
                new SettlementBatchId(UUID.randomUUID()),
                now,
                now.minusSeconds(3600),
                List.of(createItem(new PaymentAllocationId(UUID.randomUUID())))
        ));
    }

    private SettlementBatch createBatch() {
        return createBatchWithItem(createItem(new PaymentAllocationId(UUID.randomUUID())));
    }

    private SettlementBatch createBatchWithItem(SettlementBatchItem item) {
        return createBatchWithItems(List.of(item));
    }

    private SettlementBatch createBatchWithItems(List<SettlementBatchItem> items) {
        return new SettlementBatch(
                new SettlementBatchId(UUID.randomUUID()),
                Instant.now().minusSeconds(86_400),
                Instant.now(),
                items
        );
    }

    private SettlementBatchItem createItem(PaymentAllocationId allocationId) {
        SettlementLine line = new SettlementLine(
                new SettlementLineId(UUID.randomUUID()),
                allocationId,
                Money.vnd(92_000)
        );

        return new SettlementBatchItem(
                new SettlementBatchItemId(UUID.randomUUID()),
                new ShopId(UUID.randomUUID()),
                new WalletId(UUID.randomUUID()),
                Money.vnd(100_000),
                Money.vnd(7_000),
                Money.vnd(1_000),
                Money.vnd(92_000),
                Money.vnd(92_000),
                Money.vnd(0),
                List.of(line)
        );
    }
}