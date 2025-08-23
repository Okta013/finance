package ru.anikeeva.finance.repositories.budget;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.anikeeva.finance.entities.budget.Transaction;
import ru.anikeeva.finance.entities.enums.ETransactionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Page<Transaction> findAllByUserId(UUID userId, Pageable pageable);

    List<Transaction> findAllByUserIdAndTypeAndDateTimeBetween(UUID userId, ETransactionType type,
                                                               LocalDateTime startDate, LocalDateTime endDate);
}