package ru.anikeeva.finance.services.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class LoginAttemptService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ValueOperations<String, String> valueOperations;

    public LoginAttemptService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.valueOperations = redisTemplate.opsForValue();
    }

    @Value("${auth-attempt.max-attempt}")
    private int maxAttempts;

    @Value("${auth-attempt.block-time-in-seconds}")
    private int blockTimeInSeconds;

    private String attemptsKey(final String username, final String ip) {
        return "login-attempt-" + username + "-" + ip;
    }

    private String blockedKey(final String username, final String ip) {
        return "login-blocked-" + username + "-" + ip;
    }

    public boolean isBlocked(final String username, final String ip) {
        try {
            return redisTemplate.hasKey(blockedKey(username, ip));
        } catch (RedisConnectionFailureException e) {
            log.error("Соединение с Redis было потеряно во время попытки проверки блокировки " +
                "пользователя {} с ip {}: {}", username, ip, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Произошла непредвиденная ошибка во время попытки проверки блокировки " +
                "пользователя {} с ip {}: {}", username, ip, e.getMessage());
            return false;
        }
    }

    public void loginSucceeded(final String username, final String ip) {
        try {
            redisTemplate.delete(blockedKey(username, ip));
            redisTemplate.delete(attemptsKey(username, ip));
            log.info("Аутентификация пользователя {} с ip {} успешна, данные о предыдущих попытках входа очищены",
                username, ip);
        } catch (RedisConnectionFailureException e) {
            log.error("Соединение с Redis было потеряно во время записи успешной попытки аутентификации " +
                "пользователя {} с ip {}: {}", username, ip, e.getMessage());
        } catch (Exception e) {
            log.error("Непредвиденная ошибка во время записи успешной попытки аутентификации " +
                "пользователя {} с ip {}: {}", username, ip, e.getMessage());
        }
    }

    public void loginFailed(final String username, final String ip) {
        try {
            String key = attemptsKey(username, ip);
            Long attempts = valueOperations.increment(key);
            if (attempts != null && attempts == 1) {
                log.info("Число неудачных попыток входа для пользователя {} с ip {} увеличено на 1", username, ip);
                redisTemplate.expire(key, Duration.ofSeconds(blockTimeInSeconds));
            }
            if (attempts != null && attempts >= maxAttempts) {
                log.info("Пользователь {} с ip {} заблокирован на 15 минут в результате превышения числа попыток входа",
                    username, ip);
                valueOperations.set(blockedKey(username, ip), "blocked", Duration.ofSeconds(blockTimeInSeconds));
            }
        } catch (RedisConnectionFailureException e) {
            log.error("Соединение с Redis было потеряно во время записи проваленной попытки аутентификации " +
                "пользователя {} с ip {}: {}", username, ip, e.getMessage());
        } catch (Exception e) {
            log.error("Непредвиденная ошибка во время записи проваленной попытки аутентификации " +
                "пользователя {} с ip {}: {}", username, ip, e.getMessage());
        }
    }
}