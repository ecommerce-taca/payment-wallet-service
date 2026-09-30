package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.application.settlement.UnsettledSettlementCandidate;

import java.time.Instant;
import java.util.List;

public interface UnsettledSettlementCandidatePort {

    List<UnsettledSettlementCandidate> findUnsettledCandidates(
            Instant periodStart,
            Instant periodEnd
    );
}