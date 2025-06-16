package ru.anikeeva.finance.dto.budget;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.anikeeva.finance.entities.budget.Transaction;

import java.util.UUID;

@Schema(description = "Ответ на запрос создания транзакции")
public record CreateTransactionResponse(
    @Schema(description = "Уникальный идентификатор созданной транзакции")
    UUID id,

    @Schema(description = "Флаг успеха создания транзакции")
    boolean isSuccess
)
{}