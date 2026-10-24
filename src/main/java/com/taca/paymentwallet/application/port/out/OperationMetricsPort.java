package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.observability.OperationMetric;
import com.taca.paymentwallet.application.observability.OperationResult;

import java.time.Duration;

public interface OperationMetricsPort {

    void record(
            OperationMetric operation,
            OperationResult result,
            Duration duration
    );
}