package ru.anikeeva.finance.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import ru.anikeeva.finance.annotations.PasswordMatches;
import ru.anikeeva.finance.annotations.PasswordValid;

@Schema(description = "Запрос на смену пароля пользователя")
public record ChangePasswordRequest(
    @Schema(description = "Старый пароль", example = "Password12345?")
    @NotBlank
    String oldPassword,

    @Schema(description = "Новый пароль", example = "Password123?")
    @NotBlank
    @PasswordValid
    String newPassword,

    @Schema(description = "Подтверждение пароля", example = "Password123?")
    @NotBlank
    @PasswordMatches
    String confirmPassword
) {
}
