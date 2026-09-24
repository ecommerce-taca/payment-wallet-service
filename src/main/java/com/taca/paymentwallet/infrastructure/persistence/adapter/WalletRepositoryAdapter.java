package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.ClockPort;
import com.taca.paymentwallet.application.port.out.WalletRepositoryPort;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.domain.valueobject.WalletId;
import com.taca.paymentwallet.domain.wallet.Wallet;
import com.taca.paymentwallet.infrastructure.persistence.entity.WalletJpaEntity;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.mapper.WalletPersistenceMapper;
import com.taca.paymentwallet.infrastructure.persistence.repository.WalletJpaRepository;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class WalletRepositoryAdapter
        implements WalletRepositoryPort {

    private final WalletJpaRepository walletJpaRepository;
    private final WalletPersistenceMapper mapper;
    private final ClockPort clockPort;

    public WalletRepositoryAdapter(
            WalletJpaRepository walletJpaRepository,
            WalletPersistenceMapper mapper,
            ClockPort clockPort
    ) {
        this.walletJpaRepository =
                Objects.requireNonNull(walletJpaRepository);

        this.mapper =
                Objects.requireNonNull(mapper);

        this.clockPort =
                Objects.requireNonNull(clockPort);
    }

    @Override
    public Optional<Wallet> findById(
            WalletId walletId
    ) {
        Objects.requireNonNull(
                walletId,
                "walletId must not be null"
        );

        return walletJpaRepository
                .findById(walletId.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Wallet> findByIdForUpdate(
            WalletId walletId
    ) {
        Objects.requireNonNull(
                walletId,
                "walletId must not be null"
        );

        return walletJpaRepository
                .findByIdForUpdate(
                        walletId.value()
                )
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Wallet> findByShopIdAndCurrencyForUpdate(
            ShopId shopId,
            String currency
    ) {
        Objects.requireNonNull(
                shopId,
                "shopId must not be null"
        );

        return walletJpaRepository
                .findByShopIdAndCurrencyForUpdate(
                        shopId.value(),
                        normalizeCurrency(currency)
                )
                .map(mapper::toDomain);
    }

    @Override
    public Wallet save(
            Wallet wallet
    ) {
        Objects.requireNonNull(
                wallet,
                "wallet must not be null"
        );

        Optional<WalletJpaEntity> existing =
                walletJpaRepository.findById(
                        wallet.id().value()
                );

        if (existing.isPresent()) {
            return updateExisting(
                    wallet,
                    existing.get()
            );
        }

        return insertNew(wallet);
    }

    private Wallet insertNew(
            Wallet wallet
    ) {
        LocalDateTime now =
                currentPersistenceTime();

        WalletJpaEntity entity =
                mapper.toNewEntity(
                        wallet,
                        now
                );

        walletJpaRepository.save(entity);

        return wallet;
    }

    private Wallet updateExisting(
            Wallet wallet,
            WalletJpaEntity entity
    ) {
        LocalDateTime now =
                currentPersistenceTime();

        mapper.updateEntity(
                wallet,
                entity,
                now
        );

        walletJpaRepository.save(entity);

        return wallet;
    }

    private LocalDateTime currentPersistenceTime() {
        return PersistenceTimeMapper
                .toLocalDateTime(
                        clockPort.now()
                );
    }

    private String normalizeCurrency(
            String currency
    ) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException(
                    "currency must not be blank"
            );
        }

        return currency
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}