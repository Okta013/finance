package ru.anikeeva.finance.dto.mail;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ на запрос подтверждения электронной почты по токену")
public record ConfirmEmailResponse(
    @Schema(description = "Сообщение ответа")
    String message,

    @Schema(description = "Флаг успеха операции")
    boolean isSuccess
)
{}