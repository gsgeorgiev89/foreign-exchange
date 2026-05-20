package com.example.fx.repository;

import com.example.fx.domain.Conversion;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversionRepository extends JpaRepository<Conversion, UUID> {
    Page<Conversion> findByClientId(String clientId, Pageable pageable);
    Page<Conversion> findByClientIdAndCreatedAtBetween(
        String clientId, Instant from, Instant to, Pageable pageable
    );
    Page<Conversion> findByCreatedAtBetween(
        Instant from, Instant to, Pageable pageable
    );
}