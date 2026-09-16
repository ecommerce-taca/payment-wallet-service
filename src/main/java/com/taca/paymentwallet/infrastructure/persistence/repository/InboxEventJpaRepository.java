package com.taca.paymentwallet.infrastructure.persistence.repository;

import com.taca.paymentwallet.infrastructure.persistence.entity.InboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InboxEventJpaRepository extends JpaRepository<InboxEventJpaEntity, UUID> {

    Optional<InboxEventJpaEntity> findByConsumerNameAndSourceAndEventId(
            String consumerName,
            String source,
            String eventId
    );
}
