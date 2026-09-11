package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.settlement.SettlementCandidate;

import java.time.Instant;
import java.util.List;

public interface SettlementCandidatePort {

    List<SettlementCandidate> findEligibleCandidates(
            Instant periodStart,
            Instant periodEnd
    );
}