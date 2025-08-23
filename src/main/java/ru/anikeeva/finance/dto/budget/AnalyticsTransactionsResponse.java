package ru.anikeeva.finance.dto.budget;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Ответ на запрос аналитики транзакций за период")
public record AnalyticsTransactionsResponse(
    @Schema(description = "Сумма всех доходов за период")
    BigDecimal income,

    @Schema(description = "Сумма всех расходов за период")
    BigDecimal expenses
)
{}