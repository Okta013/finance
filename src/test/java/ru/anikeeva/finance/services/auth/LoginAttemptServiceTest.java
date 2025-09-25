package ru.anikeeva.finance.services.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoginAttemptServiceTest {
    @InjectMocks
    private LoginAttemptService loginAttemptService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginAttemptService, "valueOperations", valueOperations);
        ReflectionTestUtils.setField(loginAttemptService, "maxAttempts", 5);
    }

    @Test
    @DisplayName("Проверка незаблокированного пользователя")
    public void isUnblockedUserBlocked() {
        String username = "username";
        String ip = "127.0.0.1";
        boolean expected = false;

        when(redisTemplate.hasKey(any(String.class))).thenReturn(false);
        boolean actual = loginAttemptService.isBlocked(username, ip);

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Проверка заблокированного пользователя")
    public void isBlockedUserBlocked() {
        String username = "username";
        String ip = "127.0.0.1";
        boolean expected = true;

        when(redisTemplate.hasKey(any(String.class))).thenReturn(true);
        boolean actual = loginAttemptService.isBlocked(username, ip);

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Проверка блокировки пользователя при потере соединения с Redis")
    public void isBlockedWithoutRedisConnection() {
        String username = "username";
        String ip = "127.0.0.1";
        boolean expected = false;

        when(redisTemplate.hasKey(any(String.class))).thenThrow(new RedisConnectionFailureException(""));
        boolean actual = loginAttemptService.isBlocked(username, ip);

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Проверка блокировки пользователя при непредвиденной ошибке Redis")
    public void isBlockedWithUnexpectedErrorByRedis() {
        String username = "username";
        String ip = "127.0.0.1";
        boolean expected = false;

        when(redisTemplate.hasKey(any(String.class))).thenThrow(new RuntimeException());
        boolean actual = loginAttemptService.isBlocked(username, ip);

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Запись успешной аутентификации")
    public void loginSucceeded() {
        String username = "username";
        String ip = "127.0.0.1";

        when(redisTemplate.delete(any(String.class))).thenReturn(true);
        loginAttemptService.loginSucceeded(username, ip);

        verify(redisTemplate, times(2)).delete(any(String.class));
    }

    @Test
    @DisplayName("Запись проваленной попытки аутентификации при остатке лимита")
    public void loginFailedWithInexhaustibleLimit() {
        String username = "username";
        String ip = "127.0.0.1";

        when(valueOperations.increment(any(String.class))).thenReturn(2L);
        loginAttemptService.loginFailed(username, ip);

        verify(valueOperations).increment(any(String.class));
    }

    @Test
    @DisplayName("Запись проваленной попытки аутентификации при превышении лимита")
    public void loginSucceededWithExhaustibleLimit() {
        String username = "username";
        String ip = "127.0.0.1";

        when(valueOperations.increment(any(String.class))).thenReturn(6L);
        loginAttemptService.loginFailed(username, ip);

        verify(valueOperations).increment(any(String.class));
        verify(valueOperations).set(any(String.class), any(String.class), any(Duration.class));
    }

    @Test
    @DisplayName("Запись первой проваленной попытки аутентификации")
    public void loginSucceededByFirstAttempt() {
        String username = "username";
        String ip = "127.0.0.1";

        when(valueOperations.increment(any(String.class))).thenReturn(1L);
        loginAttemptService.loginFailed(username, ip);

        verify(valueOperations).increment(any(String.class));
    }
}