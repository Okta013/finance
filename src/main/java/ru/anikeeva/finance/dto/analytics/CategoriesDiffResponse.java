package ru.anikeeva.finance.dto.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.anikeeva.finance.entities.enums.ETransactionType;

@Schema(description = "Вспомогательный дто для формирования ответа на запрос разницы метрик двух периодов")
public record CategoriesDiffResponse(
    @Schema(description = "Тип транзакции - доход или расход")
    ETransactionType transactionType,

    @Schema(description = "Название категории")
    String category,

    @Schema(description = "Разница между периодами")
    String diff
)
{}