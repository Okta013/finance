package ru.anikeeva.finance.services.registration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.anikeeva.finance.dto.auth.AuthRequest;
import ru.anikeeva.finance.dto.auth.AuthResponse;
import ru.anikeeva.finance.dto.registration.SignUpRequest;
import ru.anikeeva.finance.entities.enums.ERole;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.repositories.user.UserRepository;
import ru.anikeeva.finance.services.auth.AuthenticationService;

import java.math.BigDecimal;
import java.util.Currency;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationService authenticationService;

    @Transactional
    public AuthResponse register(final SignUpRequest request, final HttpServletRequest httpServletRequest,
                                 final HttpServletResponse response) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Пользователь с указанным именем уже зарегистрирован");
        }
        User user = User.builder()
            .username(request.username())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .role(ERole.USER)
            .balance(BigDecimal.ZERO)
            .baseCurrency(Currency.getInstance("RUB"))
            .isEnabled(true)
            .isEmailActive(false)
            .isMailingAgree(false)
            .build();
        userRepository.save(user);
        log.info("Пользователь {} успешно создан", request.username());
        AuthRequest authRequest = new AuthRequest(request.username(), request.password());
        log.info("Пользователь {} автоматически аутентифицирован после регистрации", request.username());
        return authenticationService.login(authRequest, httpServletRequest, response);
    }
}