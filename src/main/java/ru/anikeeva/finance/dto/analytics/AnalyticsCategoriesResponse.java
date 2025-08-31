package ru.anikeeva.finance.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Ответ на запрос получения процентного распределения транзакций по категориям")
public record AnalyticsCategoriesResponse(
    @Schema(description = "Список из пар Название категории - Процентное соотношение от общей суммы транзакций за период")
    List<AnalyticsCategoryResponse> responses
)
{}