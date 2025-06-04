package ru.anikeeva.finance.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на аутентификацию")
public record AuthRequest(
    @Schema(description = "Имя пользователя", example = "Username")
    @NotBlank
    String username,

    @Schema(description = "Пароль пользователя", example = "UserPassword123?")
    @NotBlank
    String password
)
{}