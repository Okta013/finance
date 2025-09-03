package ru.anikeeva.finance.dto.registration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.anikeeva.finance.annotations.PasswordMatches;
import ru.anikeeva.finance.annotations.PasswordValid;

@Schema(description = "Запрос на регистрацию")
public record SignUpRequest(
    @Schema(description = "Имя пользователя", example = "Username")
    @NotBlank
    @Size(min = 3, max = 20)
    String username,

    @Schema(description = "Адрес электронной почты пользователя", example = "example@mail.ru")
    @Email
    String email,

    @Schema(description = "Пароль пользователя", example = "Password123?")
    @NotBlank
    @PasswordValid
    String password,

    @Schema(description = "Подтверждение пароля", example = "Password123?")
    @NotBlank
    @PasswordMatches
    String confirmPassword
)
{}