package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.domain.refund.RefundAllocation;

import java.util.List;

public interface RefundAllocationRepositoryPort {

    void saveAll(List<RefundAllocation> refundAllocations);
}
