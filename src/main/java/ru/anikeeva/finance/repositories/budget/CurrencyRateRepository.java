package ru.anikeeva.finance.repositories.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.anikeeva.finance.entities.budget.CurrencyRate;

import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, UUID> {
    Optional<CurrencyRate> findTopByCurrencyAndSourceOrderByUpdateDateDesc(Currency currency, String source);

    List<CurrencyRate> findByUpdatedAtAfter(LocalDateTime since);

}