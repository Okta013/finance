package ru.anikeeva.finance.schedulers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.anikeeva.finance.services.budget.CurrencyRateService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyRateScheduler {
    private final CurrencyRateService currencyRateService;

    @Scheduled(cron = "0 0 0 * * *")
    public void scheduledUpdateRates() {
        log.info("Запуск обновления курсов валют");
        currencyRateService.updateCurrencyRatesWithFallback()
            .doOnError(e -> log.error("Ошибка обновления курсов валют", e))
            .subscribe();
    }
}