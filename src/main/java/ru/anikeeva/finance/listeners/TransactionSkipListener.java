package ru.anikeeva.finance.listeners;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import ru.anikeeva.finance.dto.budget.TransactionImportDto;
import ru.anikeeva.finance.entities.budget.Transaction;

@Component
@Slf4j
public class TransactionSkipListener implements SkipListener<TransactionImportDto, Transaction> {
    @Override
    public void onSkipInRead(Throwable t) {
        log.error("Ошибка чтения: {}", t.getMessage());
    }

    @Override
    public void onSkipInWrite(@NonNull Transaction item, Throwable t) {
        log.error("Ошибка записи: {}. Ошибочный элемент: {}", t.getMessage(), item);
    }

    @Override
    public void onSkipInProcess(@NonNull TransactionImportDto item, Throwable t) {
        log.error("Ошибка при обработке элемента {}: {}", item, t.getMessage());
    }
}