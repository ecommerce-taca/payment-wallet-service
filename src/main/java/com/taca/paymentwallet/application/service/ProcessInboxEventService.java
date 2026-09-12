package com.taca.paymentwallet.application.service;

import com.taca.paymentwallet.application.command.ProcessInboxEventCommand;
import com.taca.paymentwallet.application.inbox.InboxEvent;
import com.taca.paymentwallet.application.inbox.InboxEventKey;
import com.taca.paymentwallet.application.inbox.InboxEventStatus;
import com.taca.paymentwallet.application.port.in.ProcessInboxEventUseCase;
import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.InboxEventPort;
import com.taca.paymentwallet.application.port.out.TransactionPort;
import com.taca.paymentwallet.application.result.InboxEventExecutionResult;

import java.util.Objects;
import java.util.function.Supplier;

public class ProcessInboxEventService implements ProcessInboxEventUseCase {

    private final InboxEventPort inboxEventPort;
    private final ClockPort clockPort;
    private final TransactionPort transactionPort;

    public ProcessInboxEventService(
            InboxEventPort inboxEventPort,
            ClockPort clockPort,
            TransactionPort transactionPort
    ) {
        this.inboxEventPort = Objects.requireNonNull(inboxEventPort);
        this.clockPort = Objects.requireNonNull(clockPort);
        this.transactionPort = Objects.requireNonNull(transactionPort);
    }

    @Override
    public <T> InboxEventExecutionResult<T> execute(
            ProcessInboxEventCommand command,
            Supplier<T> action
    ) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(action, "action must not be null");

        return transactionPort.execute(() -> process(command, action));
    }

    private <T> InboxEventExecutionResult<T> process(
            ProcessInboxEventCommand command,
            Supplier<T> action
    ) {
        InboxEventKey key = new InboxEventKey(
                command.consumerName(),
                command.source(),
                command.eventId()
        );

        InboxEvent event = new InboxEvent(
                key,
                command.eventType(),
                command.payloadHash(),
                clockPort.now(),
                InboxEventStatus.RECEIVED
        );

        boolean inserted = inboxEventPort.recordIfAbsent(event);

        if (!inserted) {
            return InboxEventExecutionResult.duplicate();
        }

        T result = action.get();

        inboxEventPort.markProcessed(key, clockPort.now());

        return InboxEventExecutionResult.applied(result);
    }
}