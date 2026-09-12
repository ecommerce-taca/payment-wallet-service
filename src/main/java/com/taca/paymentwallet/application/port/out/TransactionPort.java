package com.taca.paymentwallet.application.port.out;

import java.util.function.Supplier;

public interface TransactionPort {

    <T> T execute(Supplier<T> action);

    default void execute(Runnable action) {
        execute(() -> {
            action.run();
            return null;
        });
    }
}
