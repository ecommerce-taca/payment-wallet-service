package com.taca.paymentwallet.application.port.out;

import java.util.List;

public interface AdminTaxConfigReadPort {

    List<AdminTaxConfigView> findAll();
}