package ru.anikeeva.finance.dto.budget;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.anikeeva.finance.entities.enums.ETransactionCategory;
import ru.anikeeva.finance.entities.enums.ETransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

@Schema(description = "Запрос на создание транзакции")
public record CreateTransactionRequest(
    @Schema(description = "Тип транзакции - доход или расход", example = "EXPENSE")
    @NotNull
    ETransactionType type,

    @Schema(description = "Категория транзакции", example = "OTHER_EXPENSES")
    @NotNull
    ETransactionCategory category,

    @Schema(description = "Сумма транзакции", example = "200.00")
    @NotNull
    BigDecimal initialAmount,

    @Schema(description = "Валюта, по умолчанию RUB", example = "USD")
    Currency initialCurrency,

    @Schema(description = "Дата и время совершения транзакции", example = "2025-06-11T09:15:30")
    @NotNull
    LocalDateTime dateTime,

    @Schema(description = "Описание транзакции", example = "Непредвиденный расход на корм для уличного котенка")
    @Size(max = 255)
    String description
)
{
    public CreateTransactionRequest {
        if (initialCurrency == null) {
            initialCurrency = Currency.getInstance("RUB");
        }
    }
}