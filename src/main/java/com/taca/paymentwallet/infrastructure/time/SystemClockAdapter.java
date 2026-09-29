package com.taca.paymentwallet.infrastructure.time;

import com.taca.paymentwallet.application.port.out.ClockPort;

import java.time.Instant;

public class SystemClockAdapter implements ClockPort {

    @Override
    public Instant now() {
        return Instant.now();
    }
}