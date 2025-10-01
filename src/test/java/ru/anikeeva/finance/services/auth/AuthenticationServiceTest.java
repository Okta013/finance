package ru.anikeeva.finance.services.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import ru.anikeeva.finance.dto.auth.AuthRequest;
import ru.anikeeva.finance.dto.auth.AuthResponse;
import ru.anikeeva.finance.entities.auth.RefreshTokenBlackList;
import ru.anikeeva.finance.entities.enums.ERole;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.exceptions.LoginLockException;
import ru.anikeeva.finance.repositories.auth.RefreshTokenBlackListRepository;
import ru.anikeeva.finance.repositories.user.UserRepository;
import ru.anikeeva.finance.security.impl.UserDetailsServiceImpl;
import ru.anikeeva.finance.security.jwt.JwtService;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {
    @InjectMocks
    private AuthenticationService authenticationService;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private RefreshTokenBlackListRepository refreshTokenBlackListRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IpAddressService ipAddressService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @Test
    @DisplayName("Аутентификация незаблокированного пользователя с корректными данными")
    public void loginWithCorrectCredentialsOfUnblockedUser() {
        String username = "username";
        String password = "password";
        String ip = "127.0.0.1";
        String accessToken = "accessToken";
        String refreshToken = "refreshToken";
        AuthRequest authRequest = new AuthRequest(username, password);
        AuthResponse expectedAuthResponse = new AuthResponse(accessToken);

        when(ipAddressService.getClientIP(httpServletRequest)).thenReturn(ip);
        when(loginAttemptService.isBlocked(username, ip)).thenReturn(false);
        doNothing().when(loginAttemptService).loginSucceeded(username, ip);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(jwtService.generateAccessToken(userDetails)).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(userDetails)).thenReturn(refreshToken);
        AuthResponse actualAuthResponse = authenticationService.login(authRequest, httpServletRequest,
            httpServletResponse);

        assertNotNull(actualAuthResponse);
        assertEquals(expectedAuthResponse, actualAuthResponse);
        verify(loginAttemptService).loginSucceeded(username, ip);
        verify(jwtService).generateAccessToken(userDetails);
        verify(jwtService).generateRefreshToken(userDetails);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Аутентификация заблокированного пользователя с корректными данными")
    public void loginWithCorrectCredentialsOfBlockedUser() {
        String username = "username";
        String password = "password";
        String ip = "127.0.0.1";
        String expectedExceptionMessage = "Пользователь заблокирован из-за превышения числа попыток входа";
        AuthRequest authRequest = new AuthRequest(username, password);

        when(ipAddressService.getClientIP(httpServletRequest)).thenReturn(ip);
        when(loginAttemptService.isBlocked(username, ip)).thenReturn(true);
        LoginLockException thrown = assertThrows(LoginLockException.class, () ->
            authenticationService.login(authRequest, httpServletRequest, httpServletResponse));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Аутентификация с некорректными данными")
    public void loginWithIncorrectCredentialsOfBlockedUser() {
        String username = "username";
        String password = "password";
        String ip = "127.0.0.1";
        String expectedExceptionMessage = "Неверные учетные данные пользователя";
        AuthRequest authRequest = new AuthRequest(username, password);

        when(ipAddressService.getClientIP(httpServletRequest)).thenReturn(ip);
        when(loginAttemptService.isBlocked(username, ip)).thenReturn(false);
        doNothing().when(loginAttemptService).loginFailed(username, ip);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException(expectedExceptionMessage));
        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () ->
            authenticationService.login(authRequest, httpServletRequest, httpServletResponse));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
        verify(loginAttemptService).loginFailed(username, ip);
    }

    @Test
    @DisplayName("Обновление токенов с валидным refresh-токеном")
    public void refreshWithCorrectRefreshToken() {
        String refreshToken = "refreshToken";
        String username = "username";
        String newAccessToken = "newAccessToken";
        String newRefreshToken = "newRefreshToken";
        AuthResponse expectedAuthResponse = new AuthResponse(newAccessToken);
        Cookie[] cookies = new Cookie[]{ new Cookie("refreshToken", refreshToken) };

        when(httpServletRequest.getCookies()).thenReturn(cookies);
        when(jwtService.extractUsername(refreshToken)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.validateToken(refreshToken, userDetails)).thenReturn(true);
        when(refreshTokenBlackListRepository.existsByToken(any(String.class))).thenReturn(false);
        when(jwtService.generateAccessToken(userDetails)).thenReturn(newAccessToken);
        when(jwtService.generateRefreshToken(userDetails)).thenReturn(newRefreshToken);
        AuthResponse actualAuthResponse = authenticationService.refresh(httpServletRequest, httpServletResponse);

        assertNotNull(actualAuthResponse);
        assertEquals(expectedAuthResponse, actualAuthResponse);
        verify(jwtService).extractUsername(refreshToken);
        verify(userDetailsService).loadUserByUsername(username);
        verify(jwtService).validateToken(refreshToken, userDetails);
        verify(refreshTokenBlackListRepository).existsByToken(any(String.class));
        verify(jwtService).generateAccessToken(userDetails);
        verify(jwtService).generateRefreshToken(userDetails);
        verify(refreshTokenBlackListRepository).save(any(RefreshTokenBlackList.class));
    }

    @Test
    @DisplayName("Обновление токенов с пустым refresh-токеном")
    public void refreshWithEmptyRefreshToken() {
        String refreshToken = "";
        Cookie[] cookies = new Cookie[]{ new Cookie("refreshToken", refreshToken) };
        String expectedExceptionMessage = "Refresh-токен пуст";

        when(httpServletRequest.getCookies()).thenReturn(cookies);
        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () ->
            authenticationService.refresh(httpServletRequest, httpServletResponse));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
        verify(httpServletRequest, times(2)).getCookies();
    }

    @Test
    @DisplayName("Обновление токенов с невалидным refresh-токеном")
    public void refreshWithInvalidRefreshToken() {
        String refreshToken = "invalidRefreshToken";
        Cookie[] cookies = new Cookie[]{ new Cookie("refreshToken", refreshToken) };
        String username = "username";
        String expectedExceptionMessage = "Refresh-токен не прошел валидацию";

        when(httpServletRequest.getCookies()).thenReturn(cookies);
        when(jwtService.extractUsername(refreshToken)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.validateToken(refreshToken, userDetails)).thenReturn(false);
        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () ->
            authenticationService.refresh(httpServletRequest, httpServletResponse));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
        verify(httpServletRequest, times(2)).getCookies();
        verify(jwtService).extractUsername(refreshToken);
        verify(userDetailsService).loadUserByUsername(username);
        verify(jwtService).validateToken(refreshToken, userDetails);
    }

    @Test
    @DisplayName("Обновление токенов с отозванным refresh-токеном")
    public void refreshWithRevokedRefreshToken() {
        String refreshToken = "revokedRefreshToken";
        Cookie[] cookies = new Cookie[]{ new Cookie("refreshToken", refreshToken) };
        String username = "username";
        String expectedExceptionMessage = "Refresh-токен был отозван";

        when(httpServletRequest.getCookies()).thenReturn(cookies);
        when(jwtService.extractUsername(refreshToken)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.validateToken(refreshToken, userDetails)).thenReturn(true);
        when(refreshTokenBlackListRepository.existsByToken(any(String.class))).thenReturn(true);
        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () ->
            authenticationService.refresh(httpServletRequest, httpServletResponse));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
        verify(httpServletRequest, times(2)).getCookies();
        verify(jwtService).extractUsername(refreshToken);
        verify(userDetailsService).loadUserByUsername(username);
        verify(jwtService).validateToken(refreshToken, userDetails);
        verify(refreshTokenBlackListRepository).existsByToken(any(String.class));
    }

    @Test
    @DisplayName("Выход из аккаунта с корректным токеном")
    public void logoutWithCorrectRefreshToken() {
        String refreshToken = "refreshToken";
        String username = "username";
        Cookie[] cookies = new Cookie[]{ new Cookie("refreshToken", refreshToken) };
        User user = new User(UUID.randomUUID(), "username", null, "password", ERole.USER,
            BigDecimal.ZERO, Currency.getInstance("RUB"), true, false, false);

        when(httpServletRequest.getCookies()).thenReturn(cookies);
        when(jwtService.extractUsername(refreshToken)).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(jwtService.validateToken(any(String.class), any(UserDetails.class))).thenReturn(true);
        when(refreshTokenBlackListRepository.existsByToken(any(String.class))).thenReturn(false);
        authenticationService.logout(httpServletRequest);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(httpServletRequest, times(2)).getCookies();
        verify(jwtService).extractUsername(refreshToken);
        verify(userRepository).findByUsername(username);
        verify(jwtService).validateToken(any(String.class), any(UserDetails.class));
        verify(refreshTokenBlackListRepository).existsByToken(any(String.class));
        verify(refreshTokenBlackListRepository).save(any(RefreshTokenBlackList.class));
    }

    @Test
    @DisplayName("Выход из аккаунта с пустым токеном")
    public void logoutWithEmptyRefreshToken() {
        String refreshToken = "";
        String username = "username";
        Cookie[] cookies = new Cookie[]{ new Cookie("refreshToken", refreshToken) };
        User user = new User(UUID.randomUUID(), "username", null, "password", ERole.USER,
            BigDecimal.ZERO, Currency.getInstance("RUB"), true, false, false);

        when(httpServletRequest.getCookies()).thenReturn(cookies);
        when(jwtService.extractUsername(refreshToken)).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(jwtService.validateToken(any(String.class), any(UserDetails.class))).thenReturn(true);
        when(refreshTokenBlackListRepository.existsByToken(any(String.class))).thenReturn(false);
        authenticationService.logout(httpServletRequest);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(httpServletRequest, times(2)).getCookies();
        verify(jwtService).extractUsername(refreshToken);
        verify(userRepository).findByUsername(username);
        verify(jwtService).validateToken(any(String.class), any(UserDetails.class));
        verify(refreshTokenBlackListRepository).existsByToken(any(String.class));
        verify(refreshTokenBlackListRepository).save(any(RefreshTokenBlackList.class));
    }

    @Test
    @DisplayName("Выход из аккаунта с невалидным токеном")
    public void logoutWithInvalidRefreshToken() {
        String refreshToken = "invalidRefreshToken";
        String username = "username";
        Cookie[] cookies = new Cookie[]{ new Cookie("refreshToken", refreshToken) };
        User user = new User(UUID.randomUUID(), "username", null, "password", ERole.USER,
            BigDecimal.ZERO, Currency.getInstance("RUB"), true, false, false);

        when(httpServletRequest.getCookies()).thenReturn(cookies);
        when(jwtService.extractUsername(refreshToken)).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(jwtService.validateToken(any(String.class), any(UserDetails.class))).thenReturn(false);
        when(refreshTokenBlackListRepository.existsByToken(any(String.class))).thenReturn(false);
        authenticationService.logout(httpServletRequest);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(httpServletRequest, times(2)).getCookies();
        verify(jwtService).extractUsername(refreshToken);
        verify(userRepository).findByUsername(username);
        verify(jwtService).validateToken(any(String.class), any(UserDetails.class));
        verify(refreshTokenBlackListRepository).existsByToken(any(String.class));
        verify(refreshTokenBlackListRepository).save(any(RefreshTokenBlackList.class));
    }

    @Test
    @DisplayName("Выход из аккаунта с отозванным токеном")
    public void logoutWithRevokedRefreshToken() {
        String refreshToken = "revokedRefreshToken";
        String username = "username";
        Cookie[] cookies = new Cookie[]{ new Cookie("refreshToken", refreshToken) };
        User user = new User(UUID.randomUUID(), "username", null, "password", ERole.USER,
            BigDecimal.ZERO, Currency.getInstance("RUB"), true, false, false);

        when(httpServletRequest.getCookies()).thenReturn(cookies);
        when(jwtService.extractUsername(refreshToken)).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(jwtService.validateToken(any(String.class), any(UserDetails.class))).thenReturn(true);
        when(refreshTokenBlackListRepository.existsByToken(any(String.class))).thenReturn(true);
        authenticationService.logout(httpServletRequest);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(httpServletRequest, times(2)).getCookies();
        verify(jwtService).extractUsername(refreshToken);
        verify(userRepository).findByUsername(username);
        verify(jwtService).validateToken(any(String.class), any(UserDetails.class));
        verify(refreshTokenBlackListRepository).existsByToken(any(String.class));
        verify(refreshTokenBlackListRepository).save(any(RefreshTokenBlackList.class));
    }
}