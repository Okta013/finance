package ru.anikeeva.finance.services.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.repositories.user.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User findUserByUsername(final String username) {
        return userRepository.findByUsername(username).orElseThrow(() ->
            new UsernameNotFoundException("Пользователь не найден"));
    }
}