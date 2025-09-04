package ru.anikeeva.finance.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Currency;

@Schema(description = "Ответ на запрос информации профиля пользователя")
public record ReadUserResponse(
    @Schema(description = "Имя пользователя")
    String username,

    @Schema(description = "Адрес электронной почты пользователя")
    String email,

    @Schema(description = "Роль пользователя")
    String role,

    @Schema(description = "Баланс пользователя")
    BigDecimal balance,

    @Schema(description = "Базовая валюта пользователя")
    Currency baseCurrency
)
{}