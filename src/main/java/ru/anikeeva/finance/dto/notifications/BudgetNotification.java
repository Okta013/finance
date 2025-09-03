package ru.anikeeva.finance.dto.notifications;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Уведомление, связанное с бюджетом")
public record BudgetNotification(
    @Schema(description = "Сообщение")
    String message,

    @Schema(description = "Оставшийся лимит")
    BigDecimal remainingAmount,

    @Schema(description = "Категория расходов")
    String category
)
{}