package ru.anikeeva.finance.controllers.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.anikeeva.finance.dto.auth.AuthRequest;
import ru.anikeeva.finance.dto.auth.AuthResponse;
import ru.anikeeva.finance.services.auth.AuthenticationService;

@Tag(name = "Аутентификация", description = "Контроллер для управления аутентификацией пользователей")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @Operation(summary = "Аутентификация пользователя",
        description = "Принимает логин и пароль, при успехе возвращает access-токен в ответе и refresh-токен в cookie")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthRequest authRequest,
                                              HttpServletRequest httpServletRequest,
                                              HttpServletResponse response) {
        return ResponseEntity.ok(authenticationService.login(authRequest, httpServletRequest, response));
    }

    @Operation(summary = "Обновление токена доступа",
        description = "Принимает cookie с refresh-токеном, генерирует новый access-токен и refresh-токен")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(authenticationService.refresh(request, response));
    }

    @Operation(summary = "Выход из учетной записи",
        description = "Отзывает refresh-токен и производит выход из учетной записи")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authenticationService.logout(request);
        return ResponseEntity.ok().build();
    }
}