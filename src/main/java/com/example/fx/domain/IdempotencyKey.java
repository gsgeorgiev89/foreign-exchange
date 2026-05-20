package com.example.fx.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;

@Entity
@Table(name = "idempotency_keys")
@Data
public class IdempotencyKey {

    @Id
    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "conversion_id")
    private UUID conversionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    public enum Status {
        PROCESSING, COMPLETED
    }
}