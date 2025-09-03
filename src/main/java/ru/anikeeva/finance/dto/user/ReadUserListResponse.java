package ru.anikeeva.finance.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ на запрос списка всех пользователей")
public record ReadUserListResponse(
    @Schema(description = "Имя пользователя")
    String username,

    @Schema(description = "Роль пользователя")
    String role,

    @Schema(description = "Флаг неудаленного пользователя")
    Boolean isEnabled
)
{}