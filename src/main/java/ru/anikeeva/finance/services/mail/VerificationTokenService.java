package ru.anikeeva.finance.services.mail;

import lombok.RequiredArgsConstructor;
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
        return verificationToken;
    }

    public ConfirmEmailResponse confirmEmail(final String verificationToken) {
        VerificationToken token = getVerificationToken(verificationToken);
        if (token.getExpiryDate().before(new Date())) {
            return new ConfirmEmailResponse("Срок действия токена истек", false);
        }
        User user = token.getUser();
        if (user.getIsEmailActive()) {
            return new ConfirmEmailResponse("Пользователь уже активирован", false);
        }
        userService.confirmEmail(user);
        verificationTokenRepository.delete(token);
        return new ConfirmEmailResponse("Email успешно подтвержден", true);
    }

    private VerificationToken getVerificationToken(final String verificationToken) {
        return verificationTokenRepository.findByToken(verificationToken).orElseThrow(() ->
            new EntityNotFoundException("Токен подтверждения не найден"));
    }
}