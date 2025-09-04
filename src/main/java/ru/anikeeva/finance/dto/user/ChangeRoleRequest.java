package ru.anikeeva.finance.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.anikeeva.finance.entities.enums.ERole;

@Schema(description = "Запрос на изменение роли пользователя")
public record ChangeRoleRequest(
    @Schema(description = "Новая роль")
    ERole newRole
)
{}