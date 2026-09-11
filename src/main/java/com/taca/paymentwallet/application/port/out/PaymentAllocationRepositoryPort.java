package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.domain.payment.PaymentAllocation;

import java.util.List;

public interface PaymentAllocationRepositoryPort {

    void saveAll(List<PaymentAllocation> allocations);
}
