package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.ProcessInboxEventCommand;
import com.taca.paymentwallet.application.inbox.InboxEvent;
import com.taca.paymentwallet.application.inbox.InboxEventKey;
import com.taca.paymentwallet.application.inbox.InboxEventProcessingAction;
import com.taca.paymentwallet.application.inbox.InboxEventStatus;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.InboxEventPort;
import com.taca.paymentwallet.application.port.out.TransactionPort;
import com.taca.paymentwallet.application.result.InboxEventExecutionResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessInboxEventServiceTest {

    private final FakeInboxEventPort inboxEventPort = new FakeInboxEventPort();
    private final FakeClockPort clockPort = new FakeClockPort();
    private final FakeTransactionPort transactionPort = new FakeTransactionPort();

    private final ProcessInboxEventService service = new ProcessInboxEventService(
            inboxEventPort,
            clockPort,
            transactionPort
    );

    @Test
    void shouldRecordInboxEventExecuteActionAndMarkProcessed() {
        ProcessInboxEventCommand command = command();

        InboxEventExecutionResult<String> result = service.execute(
                command,
                () -> "processed-result"
        );

        InboxEventKey key = new InboxEventKey(
                command.consumerName(),
                command.source(),
                command.eventId()
        );

        Optional<InboxEvent> savedEvent = inboxEventPort.findByKey(key);

        assertThat(result.action()).isEqualTo(InboxEventProcessingAction.APPLIED);
        assertThat(result.isApplied()).isTrue();
        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.value()).isEqualTo("processed-result");

        assertThat(savedEvent).isPresent();
        assertThat(savedEvent.get().key()).isEqualTo(key);
        assertThat(savedEvent.get().eventType()).isEqualTo("shipment.delivered");
        assertThat(savedEvent.get().payloadHash()).isEqualTo("hash-001");
        assertThat(savedEvent.get().receivedAt())
                .isEqualTo(Instant.parse("2026-09-12T01:00:00Z"));
        assertThat(savedEvent.get().status()).isEqualTo(InboxEventStatus.RECEIVED);

        assertThat(inboxEventPort.processedEvents).containsEntry(
                key,
                Instant.parse("2026-09-12T01:00:01Z")
        );

        assertThat(transactionPort.executed).isTrue();
    }

    @Test
    void shouldReturnDuplicateAndNotExecuteActionWhenInboxEventAlreadyExists() {
        ProcessInboxEventCommand command = command();

        InboxEventKey key = new InboxEventKey(
                command.consumerName(),
                command.source(),
                command.eventId()
        );

        inboxEventPort.addExisting(key);

        AtomicBoolean actionExecuted = new AtomicBoolean(false);

        InboxEventExecutionResult<String> result = service.execute(
                command,
                () -> {
                    actionExecuted.set(true);
                    return "should-not-run";
                }
        );

        assertThat(result.action()).isEqualTo(InboxEventProcessingAction.DUPLICATE);
        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.isApplied()).isFalse();
        assertThat(result.value()).isNull();

        assertThat(actionExecuted).isFalse();
        assertThat(inboxEventPort.processedEvents).isEmpty();
        assertThat(transactionPort.executed).isTrue();
    }

    @Test
    void shouldNotMarkProcessedWhenActionThrowsException() {
        ProcessInboxEventCommand command = command();

        InboxEventKey key = new InboxEventKey(
                command.consumerName(),
                command.source(),
                command.eventId()
        );

        assertThatThrownBy(() -> service.execute(
                command,
                () -> {
                    throw new IllegalStateException("handler failed");
                }
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("handler failed");

        assertThat(inboxEventPort.findByKey(key)).isPresent();
        assertThat(inboxEventPort.processedEvents).doesNotContainKey(key);
        assertThat(transactionPort.executed).isTrue();
    }

    @Test
    void shouldRejectNullCommand() {
        assertThatThrownBy(() -> service.execute(null, () -> "result"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");
    }

    @Test
    void shouldRejectNullAction() {
        assertThatThrownBy(() -> service.execute(command(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("action must not be null");
    }

    private ProcessInboxEventCommand command() {
        return new ProcessInboxEventCommand(
                "payment-wallet-shipment-consumer",
                "shipment-service",
                "shipment-event-001",
                "shipment.delivered",
                "hash-001"
        );
    }

    private static class FakeInboxEventPort implements InboxEventPort {

        private final Map<InboxEventKey, InboxEvent> events = new HashMap<>();
        private final Map<InboxEventKey, Instant> processedEvents = new HashMap<>();

        void addExisting(InboxEventKey key) {
            events.put(
                    key,
                    new InboxEvent(
                            key,
                            "shipment.delivered",
                            "hash-001",
                            Instant.parse("2026-09-12T00:00:00Z"),
                            InboxEventStatus.PROCESSED
                    )
            );
        }

        Optional<InboxEvent> findByKey(InboxEventKey key) {
            return Optional.ofNullable(events.get(key));
        }

        @Override
        public boolean recordIfAbsent(InboxEvent event) {
            if (events.containsKey(event.key())) {
                return false;
            }

            events.put(event.key(), event);
            return true;
        }

        @Override
        public void markProcessed(InboxEventKey key, Instant processedAt) {
            processedEvents.put(key, processedAt);
        }
    }

    private static class FakeClockPort implements ClockPort {

        private int callCount = 0;

        @Override
        public Instant now() {
            callCount++;

            if (callCount == 1) {
                return Instant.parse("2026-09-12T01:00:00Z");
            }

            return Instant.parse("2026-09-12T01:00:01Z");
        }
    }

    private static class FakeTransactionPort implements TransactionPort {

        private boolean executed;

        @Override
        public <T> T execute(Supplier<T> action) {
            executed = true;
            return action.get();
        }
    }
}