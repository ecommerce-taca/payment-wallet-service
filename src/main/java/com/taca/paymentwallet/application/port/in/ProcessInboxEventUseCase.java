package com.taca.paymentwallet.application.port.in;

import com.taca.paymentwallet.application.command.ProcessInboxEventCommand;
import com.taca.paymentwallet.application.result.InboxEventExecutionResult;

import java.util.function.Supplier;

public interface ProcessInboxEventUseCase {

    <T> InboxEventExecutionResult<T> execute(
            ProcessInboxEventCommand command,
            Supplier<T> action
    );
}