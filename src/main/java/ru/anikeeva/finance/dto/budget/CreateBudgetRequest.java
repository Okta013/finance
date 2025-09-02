package ru.anikeeva.finance.dto.budget;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ru.anikeeva.finance.entities.enums.EBudgetPeriod;
import ru.anikeeva.finance.entities.enums.ETransactionCategory;

import java.math.BigDecimal;

@Schema(description = "Запрос на создание бюджета")
public record CreateBudgetRequest(
    @Schema(description = "Лимит по транзакциям в выбранной базовой валюте", example = "25000.00")
    @NotNull
    @Min(1)
    BigDecimal limitAmount,

    @Schema(description = "Период бюджета", example = "MONTH", allowableValues = {"DAY", "WEEK", "MONTH", "YEAR"})
    @NotNull
    EBudgetPeriod period,

    @Schema(description = "Категория расходов", example = "ENTERTAINMENT")
    @NotNull
    ETransactionCategory category
)
{}