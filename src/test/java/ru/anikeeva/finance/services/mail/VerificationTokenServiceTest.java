package ru.anikeeva.finance.services.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.anikeeva.finance.dto.mail.ConfirmEmailResponse;
import ru.anikeeva.finance.entities.enums.ERole;
import ru.anikeeva.finance.entities.mail.VerificationToken;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.exceptions.EntityNotFoundException;
import ru.anikeeva.finance.repositories.mail.VerificationTokenRepository;
import ru.anikeeva.finance.services.user.UserService;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VerificationTokenServiceTest {
    @InjectMocks
    private VerificationTokenService verificationTokenService;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private UserService userService;

    @Captor
    private ArgumentCaptor<VerificationToken> tokenCaptor;

    private static class TestVerificationTokenData {
        User user = User.builder()
            .id(UUID.randomUUID())
            .username("username")
            .password("password")
            .role(ERole.USER)
            .balance(BigDecimal.valueOf(50000))
            .baseCurrency(Currency.getInstance("RUB"))
            .isEnabled(true)
            .isEmailActive(false)
            .isMailingAgree(false)
            .build();
    }

    @Test
    @DisplayName("Генерации токена верификации")
    public void generateVerificationToken() {
        TestVerificationTokenData data = new TestVerificationTokenData();
        User user = data.user;

        String token = verificationTokenService.generateVerificationToken(user);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        verify(verificationTokenRepository).save(tokenCaptor.capture());
        VerificationToken capturedToken = tokenCaptor.getValue();
        assertEquals(token, capturedToken.getToken());
        assertEquals(user, capturedToken.getUser());
        byte[] decoded = Base64.getUrlDecoder().decode(token);
        assertEquals(24, decoded.length);
    }

    @Test
    @DisplayName("Подтверждение email по валидному токену")
    public void confirmEmailByCorrectToken() {
        TestVerificationTokenData data = new TestVerificationTokenData();
        User user = data.user;
        String verificationTokenInStr = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(UUID.randomUUID(), verificationTokenInStr, user,
            Date.valueOf(LocalDate.now().plusDays(1)));
        ConfirmEmailResponse expectedResponse = new ConfirmEmailResponse("Email успешно подтвержден", true);

        when(verificationTokenRepository.findByToken(verificationTokenInStr)).thenReturn(Optional.of(verificationToken));
        doNothing().when(userService).confirmEmail(user);
        doNothing().when(verificationTokenRepository).delete(verificationToken);
        ConfirmEmailResponse actualResponse = verificationTokenService.confirmEmail(verificationTokenInStr);

        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    @DisplayName("Подтверждение email по несуществующему токену")
    public void confirmEmailByNonExistingToken() {
        String verificationTokenInStr = UUID.randomUUID().toString();
        String expectedExceptionMessage = "Токен подтверждения не найден";

        when(verificationTokenRepository.findByToken(verificationTokenInStr)).thenReturn(Optional.empty());
        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () ->
            verificationTokenService.confirmEmail(verificationTokenInStr));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Подтверждение email по просроченному токену")
    public void confirmEmailByExpiredToken() {
        TestVerificationTokenData data = new TestVerificationTokenData();
        String verificationTokenInStr = UUID.randomUUID().toString();
        User user = data.user;
        VerificationToken verificationToken = new VerificationToken(UUID.randomUUID(), verificationTokenInStr, user,
            Date.valueOf(LocalDate.now().minusDays(1)));
        ConfirmEmailResponse expectedResponse = new ConfirmEmailResponse("Срок действия токена истек", false);

        when(verificationTokenRepository.findByToken(verificationTokenInStr)).thenReturn(Optional.of(verificationToken));
        ConfirmEmailResponse actualResponse = verificationTokenService.confirmEmail(verificationTokenInStr);

        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    @DisplayName("Подтверждение уже подтвержденного email")
    public void confirmAlreadyConfirmedEmail() {
        User user = User.builder()
            .id(UUID.randomUUID())
            .username("username")
            .password("password")
            .role(ERole.USER)
            .balance(BigDecimal.valueOf(50000))
            .baseCurrency(Currency.getInstance("RUB"))
            .isEnabled(true)
            .isEmailActive(true)
            .isMailingAgree(true)
            .build();
        String verificationTokenInStr = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(UUID.randomUUID(), verificationTokenInStr, user,
            Date.valueOf(LocalDate.now().plusDays(1)));
        ConfirmEmailResponse expectedResponse = new ConfirmEmailResponse("Адрес электронной почты уже подтвержден", false);

        when(verificationTokenRepository.findByToken(verificationTokenInStr)).thenReturn(Optional.of(verificationToken));
        ConfirmEmailResponse actualResponse = verificationTokenService.confirmEmail(verificationTokenInStr);

        assertEquals(expectedResponse, actualResponse);
    }
}