package ru.anikeeva.finance.controllers.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.anikeeva.finance.dto.mail.ConfirmEmailResponse;
import ru.anikeeva.finance.dto.user.ChangePasswordRequest;
import ru.anikeeva.finance.dto.user.ChangeRoleRequest;
import ru.anikeeva.finance.dto.user.ReadUserListResponse;
import ru.anikeeva.finance.dto.user.ReadUserResponse;
import ru.anikeeva.finance.dto.user.UpdateUserRequest;
import ru.anikeeva.finance.security.impl.UserDetailsImpl;
import ru.anikeeva.finance.services.auth.PasswordService;
import ru.anikeeva.finance.services.mail.MailService;
import ru.anikeeva.finance.services.mail.VerificationTokenService;
import ru.anikeeva.finance.services.user.UserService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Контроллер управления пользователями")
public class UserController {
    private final UserService userService;
    private final PasswordService passwordService;
    private final MailService mailService;
    private final VerificationTokenService verificationTokenService;

    @GetMapping("/{id}")
    @Operation(summary = "Просмотр профиля пользователя",
        description = "Администраторам доступны для просмотра все профили, остальным - только свой")
    public ResponseEntity<ReadUserResponse> readUserProfile(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                                            @PathVariable UUID id) {
        return ResponseEntity.ok(userService.readUserProfile(currentUser, id));
    }

    @GetMapping
    @Operation(summary = "Просмотр списка всех пользователей",
        description = "Загружает всех пользователей с пагинацией, доступно только администраторам")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReadUserListResponse>> getAllUsers(
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "20") int size,
        @RequestParam(required = false, defaultValue = "null") Boolean enabledFilter
    ) {
        return ResponseEntity.ok(userService.getAllUsers(page, size, enabledFilter));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Изменение информации профиля",
        description = "Позволяет изменить имя пользователя, email, базовую валюту. " +
            "Администратор может изменить профиль любого пользователя, остальные - только свой")
    public ResponseEntity<Void> updateUser(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                                        @PathVariable UUID id,
                                                        @RequestBody @Valid UpdateUserRequest request) {
        userService.updateUser(currentUser, id, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/change/password")
    @Operation(summary = "Смена пароля", description = "Всем пользователям доступно только изменение своего пароля")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                               @RequestBody @Valid ChangePasswordRequest request) {
        passwordService.changePassword(currentUser, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/change/role/{id}")
    @Operation(summary = "Смена роли", description = "Позволяет администраторам менять роль пользователя")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeRole(@PathVariable UUID id, @RequestBody ChangeRoleRequest changeRoleRequest) {
        userService.changeRole(id, changeRoleRequest);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Удаление пользователя", description = "Производит мягкое удаление пользователя")
    public ResponseEntity<Void> deleteProfile(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        userService.deleteProfile(currentUser);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/change/active/{id}")
    @Operation(summary = "Смена статуса активности пользователя",
        description = "Позволяет сделать пользователя активным или неактивным, доступно только администратору")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeActive(@PathVariable UUID id) {
        userService.changeActive(id);
        return ResponseEntity.noContent().build();
    }
}