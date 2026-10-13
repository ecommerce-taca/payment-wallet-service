package com.taca.paymentwallet.infrastructure.persistence.projection;

import java.time.LocalDate;

public interface SellerRevenueAllocationProjection {

    LocalDate getDate();

    Long getGross();

    Long getCommission();

    Long getTax();

    Long getNet();

    Long getOrderCount();
}