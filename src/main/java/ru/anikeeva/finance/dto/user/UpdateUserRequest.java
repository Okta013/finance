package ru.anikeeva.finance.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.Currency;

@Schema(description = "Запрос на изменение информации профиля")
public record UpdateUserRequest(
    @Schema(description = "Имя пользователя")
    @Size(min = 3, max = 20)
    String username,

    @Schema(description = "Адрес электронной почты пользователя")
    @Email
    String email,

    @Schema(description = "Базовая валюта пользователя")
    Currency baseCurrency
)
{}