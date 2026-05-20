package com.example.fx.repository;

import com.example.fx.domain.Balance;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BalanceRepository extends JpaRepository<Balance, Long> {

    Optional<Balance> findByClientIdAndCurrency(String clientId, String currency);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Balance b WHERE b.clientId = :clientId AND b.currency = :currency")
    Optional<Balance> findByClientIdAndCurrencyForUpdate(
        @Param("clientId") String clientId,
        @Param("currency") String currency
    );

    List<Balance> findAllByClientId(String clientId);
}