package ru.anikeeva.finance.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ на запрос аутентификации")
public record AuthResponse(
    @Schema(description = "Имя пользователя, прошедшего аутентификацию")
    String username,

    @Schema(description = "Токен доступа пользователя, прошедшего аутентификацию")
    String accessToken
)
{}