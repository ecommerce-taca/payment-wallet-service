package com.taca.paymentwallet.application.port.out;

import com.taca.paymentwallet.domain.valueobject.ShopId;

import java.time.LocalDate;
import java.util.List;

public interface SellerRevenueReadPort {

    List<SellerRevenueDailyView> findDailyRevenue(
            ShopId shopId,
            LocalDate from,
            LocalDate to
    );
}