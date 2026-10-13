package com.taca.paymentwallet.infrastructure.persistence.projection;

import java.time.LocalDate;

public interface SellerRevenueRefundProjection {

    LocalDate getDate();

    Long getRefunded();
}