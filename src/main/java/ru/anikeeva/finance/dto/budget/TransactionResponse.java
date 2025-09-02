package ru.anikeeva.finance.dto.budget;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.anikeeva.finance.entities.budget.Transaction;
import ru.anikeeva.finance.entities.enums.ETransactionCategory;
import ru.anikeeva.finance.entities.enums.ETransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.UUID;

@Schema(description = "Запрос просмотра транзакции")
public record TransactionResponse(
    @Schema(description = "Уникальный идентификатор транзакции")
    UUID id,

    @Schema(description = "Тип транзакции - доход или расход")
    ETransactionType type,

    @Schema(description = "Категория доходов или расходов")
    ETransactionCategory category,

    @Schema(description = "Сумма транзакции")
    BigDecimal initialAmount,

    @Schema(description = "Валюта транзакции (по умолчанию - RUB)")
    Currency initialCurrency,

    @Schema(description = "Дата и время совершения транзакции")
    LocalDateTime dateTime,

    @Schema(description = "Описание транзакции")
    String description
)
{
    public static TransactionResponse fromTransaction(Transaction transaction) {
        return new TransactionResponse(
            transaction.getId(),
            transaction.getType(),
            transaction.getCategory(),
            transaction.getInitialAmount(),
            transaction.getInitialCurrency(),
            transaction.getDateTime(),
            transaction.getDescription()
        );
    }
}