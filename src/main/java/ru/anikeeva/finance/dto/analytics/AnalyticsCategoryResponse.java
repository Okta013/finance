package ru.anikeeva.finance.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Вспомогательный для дто для формирования аналитики транзакций по категориям")
public record AnalyticsCategoryResponse(
    @Schema(description = "Название категории")
    String category,

    @Schema(description = "Процентное сообщение от общей суммы транзакций за период")
    Double percent
)
{}