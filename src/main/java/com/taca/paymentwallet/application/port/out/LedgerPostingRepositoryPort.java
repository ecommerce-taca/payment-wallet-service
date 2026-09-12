package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.domain.wallet.LedgerPosting;

public interface LedgerPostingRepositoryPort {

    void save(LedgerPosting posting);
}