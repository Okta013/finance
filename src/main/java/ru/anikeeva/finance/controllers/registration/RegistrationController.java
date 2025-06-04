package ru.anikeeva.finance.controllers.registration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.anikeeva.finance.dto.auth.AuthResponse;
import ru.anikeeva.finance.dto.registration.SignUpRequest;
import ru.anikeeva.finance.services.registration.RegistrationService;

@Tag(name = "Регистрация", description = "Контроллер управления регистрацией пользователя")
@RestController
@RequestMapping("/api/v1/registration")
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;

    @Operation(summary = "Регистрация пользователя",
        description = "Принимает имя пользователя, пароль и подтверждения пароля, при успехе регистрирует пользователя " +
            "и аутентифицирует его")
    @PostMapping
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid SignUpRequest signUpRequest,
                                                 HttpServletResponse response) {
        return ResponseEntity.ok(registrationService.register(signUpRequest, response));
    }
}