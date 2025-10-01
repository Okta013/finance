package ru.anikeeva.finance.services.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.anikeeva.finance.dto.user.ChangePasswordRequest;
import ru.anikeeva.finance.entities.enums.ERole;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.repositories.user.UserRepository;
import ru.anikeeva.finance.security.impl.UserDetailsImpl;
import ru.anikeeva.finance.services.user.UserService;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PasswordServiceTest {
    @InjectMocks
    private PasswordService passwordService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("Смена пароля при вводе верного старого пароля")
    public void changePasswordWithCorrectOldPassword() {
        String oldPassword = "oldPassword";
        String newPassword = "newPassword";
        String confirmPassword = "confirmPassword";
        UUID uuid = UUID.randomUUID();
        String username = "username";
        String expectedEncodedNewPassword = "encodedNewPassword";
        GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(ERole.USER.name());
        User user = new User(uuid, username, null, oldPassword, ERole.USER, BigDecimal.ZERO,
            Currency.getInstance("RUB"), true, false, false);
        UserDetailsImpl currentUser = new UserDetailsImpl(uuid, username, oldPassword, grantedAuthority, true);
        ChangePasswordRequest request = new ChangePasswordRequest(oldPassword, newPassword, confirmPassword);

        when(userService.findUserById(currentUser.getId())).thenReturn(user);
        when(passwordEncoder.matches(request.oldPassword(), user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(expectedEncodedNewPassword);
        passwordService.changePassword(currentUser, request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(expectedEncodedNewPassword, userCaptor.getValue().getPassword());
    }

    @Test
    @DisplayName("Смена пароля при вводе неверного старого пароля")
    public void changePasswordWithIncorrectOldPassword() {
        String oldPassword = "oldPassword";
        String incorrectOldPassword = "incorrectOldPassword";
        String newPassword = "newPassword";
        String confirmPassword = "confirmPassword";
        UUID uuid = UUID.randomUUID();
        String username = "username";
        String expectedExceptionMessage = "Старый пароль неверен";
        GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(ERole.USER.name());
        User user = new User(uuid, username, null, oldPassword, ERole.USER, BigDecimal.ZERO,
            Currency.getInstance("RUB"), true, false, false);
        UserDetailsImpl currentUser = new UserDetailsImpl(uuid, username, oldPassword, grantedAuthority, true);
        ChangePasswordRequest request = new ChangePasswordRequest(incorrectOldPassword, newPassword, confirmPassword);

        when(userService.findUserById(currentUser.getId())).thenReturn(user);
        when(passwordEncoder.matches(request.oldPassword(), user.getPassword())).thenReturn(false);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
            passwordService.changePassword(currentUser, request));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }
}