package ru.anikeeva.finance.services.budget;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import ru.anikeeva.finance.dto.budget.TransactionImportDto;
import ru.anikeeva.finance.entities.budget.Transaction;
import ru.anikeeva.finance.entities.enums.ETransactionCategory;
import ru.anikeeva.finance.entities.enums.ETransactionType;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.exceptions.BadDataException;
import ru.anikeeva.finance.services.user.UserService;

import java.util.Currency;
import java.util.UUID;

@Component
@StepScope
public class TransactionProcessor implements ItemProcessor<TransactionImportDto, Transaction> {
    private final UUID userId;
    private final UserService userService;
    private Long jobId;

    public TransactionProcessor(@Value("#{jobParameters['userId']}") String userIdStr,
                                UserService userService) {
        this.userId = UUID.fromString(userIdStr);
        this.userService = userService;
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.jobId = stepExecution.getJobExecution().getJobId();
    }

    @Override
    public Transaction process(@NonNull TransactionImportDto item) throws Exception {
        if (userId == null) {
            throw new BadDataException("Id пользователя не найдено в параметрах Job");
        }
        User currentUser = userService.findUserById(userId);
        if (item.amount() == null || item.amount().doubleValue() < 0) {
            throw new BadDataException("Сумма транзакции некорректна");
        }
        return Transaction.builder()
            .id(UUID.randomUUID())
            .user(currentUser)
            .type(ETransactionType.valueOf(item.type()))
            .category(ETransactionCategory.valueOf(item.category().toUpperCase()))
            .amount(item.amount())
            .currency(Currency.getInstance(item.currency().toUpperCase()))
            .dateTime(item.date())
            .description(item.description())
            .jobId(this.jobId)
            .build();
    }
}