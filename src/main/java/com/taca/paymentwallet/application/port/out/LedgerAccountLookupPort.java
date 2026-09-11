package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.domain.valueobject.LedgerAccountId;
import com.taca.paymentwallet.domain.valueobject.PaymentAllocationId;
import com.taca.paymentwallet.domain.valueobject.ShopId;

import java.util.List;
import java.util.Map;

public interface LedgerAccountLookupPort {

    LedgerAccountId vnpayClearingAccount();

    LedgerAccountId platformCommissionAccount();

    LedgerAccountId taxPayableAccount();

    Map<ShopId, LedgerAccountId> sellerPendingAccountsFor(List<ShopId> shopIds);

    LedgerAccountId refundClearingAccount();

    Map<PaymentAllocationId, LedgerAccountId> sellerRefundAccountsFor(
            List<PaymentAllocationId> paymentAllocationIds
    );
}
