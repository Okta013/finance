package ru.anikeeva.finance.services.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.anikeeva.finance.dto.user.ChangePasswordRequest;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.repositories.user.UserRepository;
import ru.anikeeva.finance.security.impl.UserDetailsImpl;
import ru.anikeeva.finance.services.user.UserService;

@Service
@RequiredArgsConstructor
public class PasswordService {
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final UserRepository userRepository;

    public void changePassword(final UserDetailsImpl currentUser, final ChangePasswordRequest request) {
        User user = userService.findUserById(currentUser.getId());
        if (!passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Старый пароль неверен");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}