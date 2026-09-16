package com.taca.paymentwallet.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inbox_events")
public class InboxEventJpaEntity extends UuidEntity {

    @Column(name = "consumer_name", nullable = false, length = 120)
    private String consumerName;

    @Column(name = "source", nullable = false, length = 120)
    private String source;

    @Column(name = "event_id", nullable = false, length = 160)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "payload_hash", nullable = false, length = 128)
    private String payloadHash;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "failure_code", length = 80)
    private String failureCode;
}