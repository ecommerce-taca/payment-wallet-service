package com.taca.paymentwallet.application.result;

import com.taca.paymentwallet.application.inbox.InboxEventProcessingAction;

public record InboxEventExecutionResult<T>(
        InboxEventProcessingAction action,
        T value
) {

    public InboxEventExecutionResult {
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
    }

    public static <T> InboxEventExecutionResult<T> applied(T value) {
        return new InboxEventExecutionResult<>(
                InboxEventProcessingAction.APPLIED,
                value
        );
    }

    public static <T> InboxEventExecutionResult<T> duplicate() {
        return new InboxEventExecutionResult<>(
                InboxEventProcessingAction.DUPLICATE,
                null
        );
    }

    public boolean isApplied() {
        return action == InboxEventProcessingAction.APPLIED;
    }

    public boolean isDuplicate() {
        return action == InboxEventProcessingAction.DUPLICATE;
    }
}
