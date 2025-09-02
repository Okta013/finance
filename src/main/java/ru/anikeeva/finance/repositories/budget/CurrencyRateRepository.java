package ru.anikeeva.finance.repositories.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.anikeeva.finance.entities.budget.CurrencyRate;
import ru.anikeeva.finance.entities.enums.ECurrencySource;

import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, UUID> {
    Optional<CurrencyRate> findTopByCurrencyAndSourceOrderByUpdatedAtDesc(Currency currency, ECurrencySource source);

    List<CurrencyRate> findByUpdatedAtAfter(LocalDateTime since);
}