package ru.anikeeva.finance.services.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.anikeeva.finance.dto.auth.AuthRequest;
import ru.anikeeva.finance.dto.auth.AuthResponse;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.entities.auth.RefreshTokenBlackList;
import ru.anikeeva.finance.repositories.user.UserRepository;
import ru.anikeeva.finance.repositories.auth.RefreshTokenBlackListRepository;
import ru.anikeeva.finance.security.impl.UserDetailsImpl;
import ru.anikeeva.finance.security.impl.UserDetailsServiceImpl;
import ru.anikeeva.finance.security.jwt.JwtService;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final RefreshTokenBlackListRepository refreshTokenBlackListRepository;
    private final UserRepository userRepository;

    private static final int REFRESH_TOKEN_EXPIRE = 60 * 24 * 60 * 60;

    @Transactional
    public AuthResponse login(final AuthRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("Неверное сочетание логина и пароля");
        }
        log.info("Проводится аутентификация пользователя {}", request.username());
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        addTokenToCookie(response, refreshToken);
        log.info("Access- и Refresh-токены были выданы пользователю {}", request.username());
        return new AuthResponse(accessToken);
    }

    @Transactional
    public AuthResponse refresh(final HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new BadCredentialsException("Refresh-токен пуст");
        }
        String username = jwtService.extractUsername(refreshToken);
        log.info("Обновление токена доступа для пользователя {}", username);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtService.validateToken(refreshToken, userDetails)) {
            throw new BadCredentialsException("Refresh-токен не прошел валидацию");
        }
        if (refreshTokenBlackListRepository.existsByToken(generateTokenHash(refreshToken))) {
            throw new BadCredentialsException("Refresh-токен был отозван");
        }
        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);
        addTokenToCookie(response, newRefreshToken);
        log.info("Пользователю {} были выданы новые токены", username);
        refreshTokenBlackListRepository.save(RefreshTokenBlackList.builder()
            .token(generateTokenHash(refreshToken))
            .build());
        log.info("Предыдущий refresh-токен пользователя {} был отозван", username);
        return new AuthResponse(newAccessToken);
    }

    @Transactional
    public void logout(final HttpServletRequest request) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new BadCredentialsException("Refresh-токен не найден");
        }
        User user = userRepository.findByUsername(jwtService.extractUsername(refreshToken)).orElseThrow(() ->
            new UsernameNotFoundException("Пользователь не найден"));
        log.info("Попытка выхода из аккаунта для пользователя {}", user.getUsername());
        if (!jwtService.validateToken(refreshToken, UserDetailsImpl.build(user))) {
            throw new BadCredentialsException("Refresh-токен не валиден");
        }
        String tokenHash = generateTokenHash(refreshToken);
        if (refreshTokenBlackListRepository.existsByToken(tokenHash)) {
            throw new BadCredentialsException("Refresh-токен был отозван");
        }
        refreshTokenBlackListRepository.save(RefreshTokenBlackList.builder()
            .token(tokenHash)
            .build());
        log.info("Refresh-токен пользователя {} был отозван, произошел выход из аккаунта", user.getUsername());
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    private void addTokenToCookie(final HttpServletResponse response, final String token) {
        Cookie refreshTokenCookie = new Cookie("refreshToken", token);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(REFRESH_TOKEN_EXPIRE);
        response.addCookie(refreshTokenCookie);
    }

    private String extractRefreshTokenFromCookie(final HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
            .filter(cookie -> "refreshToken".equals(cookie.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }

    private String generateTokenHash(final String refreshToken) {
        return DigestUtils.sha256Hex(refreshToken);
    }
}