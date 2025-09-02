package ru.anikeeva.finance.services.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.exceptions.EntityNotFoundException;
import ru.anikeeva.finance.repositories.user.UserRepository;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    public User findUserByUsername(final String username) {
        return userRepository.findByUsername(username).orElseThrow(() ->
            new UsernameNotFoundException("Пользователь не найден"));
    }

    public User findUserById(final UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
    }

    @Transactional
    public void recalculateBalance(UUID id, BigDecimal changes) {
        userRepository.updateBalance(id, changes);
        log.info("Баланс пользователя {} пересчитан после импорта файла транзакций на общую сумму {}", id, changes);
    }
}