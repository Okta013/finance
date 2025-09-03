package ru.anikeeva.finance.dto.budget;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.anikeeva.finance.entities.enums.EBudgetPeriod;
import ru.anikeeva.finance.entities.enums.ETransactionCategory;

import java.math.BigDecimal;

@Schema(description = "Ответ на запрос информации по бюджету")
public record ReadBudgetResponse(
    @Schema(description = "Лимит бюджета")
    BigDecimal limitAmount,

    @Schema(description = "Период бюджета")
    EBudgetPeriod period,

    @Schema(description = "Категория расходов")
    ETransactionCategory category
)
{}