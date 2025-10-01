package ru.anikeeva.finance.services.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.anikeeva.finance.dto.user.ChangePasswordRequest;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.repositories.user.UserRepository;
import ru.anikeeva.finance.security.impl.UserDetailsImpl;
import ru.anikeeva.finance.services.user.UserService;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final UserRepository userRepository;

    public void changePassword(final UserDetailsImpl currentUser, final ChangePasswordRequest request) {
        User user = userService.findUserById(currentUser.getId());
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            log.info("Попытка смены пароля пользователем {} с указанием неверного старого пароля",
                currentUser.getUsername());
            throw new IllegalArgumentException("Старый пароль неверен");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("Пароль пользователя {} успешно изменен", currentUser.getUsername());
    }
}