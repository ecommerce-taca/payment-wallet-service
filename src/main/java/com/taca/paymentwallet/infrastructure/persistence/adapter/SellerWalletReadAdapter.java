package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.SellerWalletReadPort;
import com.taca.paymentwallet.application.port.out.SellerWalletView;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.WalletJpaRepository;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class SellerWalletReadAdapter implements SellerWalletReadPort {

    private final WalletJpaRepository repository;

    public SellerWalletReadAdapter(WalletJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public Optional<SellerWalletView> findByShopIdAndCurrency(
            ShopId shopId,
            String currency
    ) {
        Objects.requireNonNull(shopId, "shopId must not be null");

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }

        return repository
                .findByShopIdAndCurrency(
                        shopId.value(),
                        currency.trim().toUpperCase(Locale.ROOT)
                )
                .map(entity -> new SellerWalletView(
                        entity.getId(),
                        entity.getShopId(),
                        entity.getAvailableBalance(),
                        entity.getPendingBalance(),
                        entity.getCurrency(),
                        entity.getStatus(),
                        PersistenceTimeMapper.toInstant(entity.getUpdatedAt())
                ));
    }
}