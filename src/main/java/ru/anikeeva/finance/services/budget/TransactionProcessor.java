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

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

@Component
@StepScope
public class TransactionProcessor implements ItemProcessor<TransactionImportDto, Transaction> {
    private final UUID userId;
    private final UserService userService;
    private Long jobId;
    private final TransactionService transactionService;

    public TransactionProcessor(@Value("#{jobParameters['userId']}") String userIdStr,
                                UserService userService, TransactionService transactionService) {
        this.userId = UUID.fromString(userIdStr);
        this.userService = userService;
        this.transactionService = transactionService;
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
        if (item.initialAmount() == null || item.initialAmount().doubleValue() < 0) {
            throw new BadDataException("Сумма транзакции некорректна");
        }
        Currency initialCurrency = Currency.getInstance(item.initialCurrency().toUpperCase());
        BigDecimal amountInBaseCurrency = initialCurrency.equals(currentUser.getBaseCurrency())
            ? item.initialAmount()
            : transactionService.calculateAmountWithBaseCurrency(currentUser, item.initialAmount(), initialCurrency);
        return Transaction.builder()
            .id(UUID.randomUUID())
            .user(currentUser)
            .type(ETransactionType.valueOf(item.type()))
            .category(ETransactionCategory.valueOf(item.category().toUpperCase()))
            .initialAmount(item.initialAmount())
            .initialCurrency(initialCurrency)
            .amountInBaseCurrency(amountInBaseCurrency)
            .dateTime(item.dateTime())
            .description(item.description())
            .jobId(this.jobId)
            .build();
    }
}