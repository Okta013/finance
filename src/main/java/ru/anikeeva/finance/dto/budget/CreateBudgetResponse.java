package ru.anikeeva.finance.dto.budget;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Ответ на запрос создания бюджета")
public record CreateBudgetResponse(
    @Schema(description = "ID созданного бюджета")
    UUID id,

    @Schema(description = "Флаг успеха создания")
    boolean isSuccess
)
{}