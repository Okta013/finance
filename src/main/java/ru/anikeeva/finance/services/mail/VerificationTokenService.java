package ru.anikeeva.finance.services.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.anikeeva.finance.dto.mail.ConfirmEmailResponse;
import ru.anikeeva.finance.entities.mail.VerificationToken;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.exceptions.EntityNotFoundException;
import ru.anikeeva.finance.repositories.mail.VerificationTokenRepository;
import ru.anikeeva.finance.services.user.UserService;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationTokenService {
    private final VerificationTokenRepository verificationTokenRepository;
    private final UserService userService;

    @Value("${spring.mail.minutes_to_confirm}")
    private int minutesToConfirm;

    public String generateVerificationToken(final User user) {
        SecureRandom random = new SecureRandom();
        byte[] randomBytes = new byte[24];
        random.nextBytes(randomBytes);
        String verificationToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        verificationTokenRepository.save(
            VerificationToken.builder()
                .token(verificationToken)
                .user(user)
                .expiryTimeInMinutes(minutesToConfirm)
                .build()
        );
        log.info("Токен верификации был создан для пользователя {}", user.getUsername());
        return verificationToken;
    }

    public ConfirmEmailResponse confirmEmail(final String verificationToken) {
        VerificationToken token = getVerificationToken(verificationToken);
        if (token.getExpiryDate().before(new Date())) {
            log.info("Попытка подтверждения email по просроченному токену {}", verificationToken);
            return new ConfirmEmailResponse("Срок действия токена истек", false);
        }
        User user = token.getUser();
        if (user.getIsEmailActive()) {
            log.info("Попытка подтверждения email {}, который уже был подтвержден пользователем {}", user.getEmail(),
                user.getUsername());
            return new ConfirmEmailResponse("Адрес электронной почты уже подтвержден", false);
        }
        userService.confirmEmail(user);
        log.info("Email {} пользователя {} успешно подтвержден", user.getEmail(), user.getUsername());
        verificationTokenRepository.delete(token);
        log.info("Токен верификации {} был удален после успешного подтверждения email {} пользователем {}",
            verificationToken, user.getEmail(), user.getUsername());
        return new ConfirmEmailResponse("Email успешно подтвержден", true);
    }

    private VerificationToken getVerificationToken(final String verificationToken) {
        return verificationTokenRepository.findByToken(verificationToken).orElseThrow(() ->
            new EntityNotFoundException("Токен подтверждения не найден"));
    }
}