package ru.anikeeva.finance.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Запрос на выход из аккаунта")
public record LogoutRequest(
    @Schema(description = "Refresh-токен, который будет отозван после выхода из аккаунта")
    String refreshToken
)
{}