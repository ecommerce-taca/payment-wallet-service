package com.taca.paymentwallet.infrastructure.persistence.adapter;

import com.taca.paymentwallet.application.port.out.SellerLedgerEntryView;
import com.taca.paymentwallet.application.port.out.SellerLedgerPage;
import com.taca.paymentwallet.application.port.out.SellerLedgerReadPort;
import com.taca.paymentwallet.domain.valueobject.ShopId;
import com.taca.paymentwallet.infrastructure.persistence.mapper.PersistenceTimeMapper;
import com.taca.paymentwallet.infrastructure.persistence.projection.SellerLedgerEntryProjection;
import com.taca.paymentwallet.infrastructure.persistence.repository.LedgerEntryJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class SellerLedgerReadAdapter implements SellerLedgerReadPort {

    private static final LocalDateTime MIN_TIME =
            LocalDateTime.of(1970, 1, 1, 0, 0);

    private static final LocalDateTime MAX_TIME =
            LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    private final LedgerEntryJpaRepository repository;

    public SellerLedgerReadAdapter(LedgerEntryJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public SellerLedgerPage findByShop(
            ShopId shopId,
            int page,
            int size,
            Instant from,
            Instant to,
            String type
    ) {
        Objects.requireNonNull(shopId, "shopId must not be null");

        Page<SellerLedgerEntryProjection> result = repository.findSellerLedger(
                shopId.value(),
                from == null ? MIN_TIME : PersistenceTimeMapper.toLocalDateTime(from),
                to == null ? MAX_TIME : PersistenceTimeMapper.toLocalDateTime(to),
                type == null ? "" : type,
                PageRequest.of(page - 1, size)
        );

        return new SellerLedgerPage(
                result.getContent().stream().map(this::toView).toList(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private SellerLedgerEntryView toView(SellerLedgerEntryProjection row) {
        return new SellerLedgerEntryView(
                UUID.fromString(row.getEntryId()),
                UUID.fromString(row.getPostingId()),
                row.getEntryType(),
                row.getAmount(),
                row.getBalanceAfter(),
                row.getReferenceType(),
                UUID.fromString(row.getReferenceId()),
                row.getDescription(),
                PersistenceTimeMapper.toInstant(row.getCreatedAt())
        );
    }
}