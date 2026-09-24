package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.domain.refund.RefundAllocation;
import com.taca.paymentwallet.domain.valueobject.RefundId;

import java.util.List;

public interface RefundAllocationRepositoryPort {

    void saveAll(
            RefundId refundId,
            List<RefundAllocation> refundAllocations
    );
}
