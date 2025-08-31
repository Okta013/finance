package ru.anikeeva.finance.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Ответ на запрос сравнения метрик двух периодов")
public record AnalyticsMetricsResponse(
    @Schema(description = "Разница в доходах")
    String incomeDiff,

    @Schema(description = "Разница в расходах")
    String expenseDiff,

    @Schema(description = "Список разниц по категориям доходов")
    List<CategoriesDiffResponse> incomeCategoriesDiffResponses,

    @Schema(description = "Список разниц по категориям расходов")
    List<CategoriesDiffResponse> expenseCategoriesDiffResponses
)
{}