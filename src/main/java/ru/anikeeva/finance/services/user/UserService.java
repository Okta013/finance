package ru.anikeeva.finance.services.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.anikeeva.finance.dto.user.ChangeRoleRequest;
import ru.anikeeva.finance.dto.user.ReadUserListResponse;
import ru.anikeeva.finance.dto.user.ReadUserResponse;
import ru.anikeeva.finance.dto.user.UpdateUserRequest;
import ru.anikeeva.finance.entities.enums.ERole;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.exceptions.EntityNotFoundException;
import ru.anikeeva.finance.exceptions.NoRightsException;
import ru.anikeeva.finance.mappers.UserMapper;
import ru.anikeeva.finance.repositories.user.UserRepository;
import ru.anikeeva.finance.security.impl.UserDetailsImpl;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public User findUserByUsername(final String username) {
        return userRepository.findByUsername(username).orElseThrow(() ->
            new UsernameNotFoundException("Пользователь не найден"));
    }

    public User findUserById(final UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
    }

    @Transactional
    public void recalculateBalance(final UUID id, final BigDecimal changes) {
        userRepository.updateBalance(id, changes);
        log.info("Баланс пользователя {} пересчитан после импорта файла транзакций на общую сумму {}", id, changes);
    }

    public ReadUserResponse readUserProfile(final UserDetailsImpl currentUser, final UUID id) {
        User user = findUserById(currentUser.getId());
        checkRightsForActionsWithUsers(user, id);
        return userMapper.toReadUserResponse(user);
    }

    public Page<ReadUserListResponse> getAllUsers(final int page, final int size, final Boolean filter) {
        Page<User> users = userRepository.findAll(PageRequest.of(page, size));
        if (filter != null && filter) users.filter(User::getIsEnabled);
        else if (filter != null && !filter) users.filter(u -> u.getIsEnabled() == false);
        return users.map(userMapper::toReadUserListResponse);
    }

    public void updateUser(final UserDetailsImpl currentUser, final UUID id, final UpdateUserRequest request) {
        User user = findUserById(currentUser.getId());
        checkRightsForActionsWithUsers(user, id);
        if (request.username() == null && request.email() == null && request.baseCurrency() == null) {
            log.info("Попытка изменения профиля пользователя {} с пустым запросом", currentUser.getId());
            throw new IllegalArgumentException("Запрос на изменение профиля пуст");
        }
        userMapper.updateUserFromUpdateUserRequest(request, user);
        userRepository.save(user);
        log.info("Детали профиля пользователя {} изменены по его запросу", currentUser.getUsername());
    }

    public void changeRole(final UUID id, final ChangeRoleRequest request) {
        User user = findUserById(id);
        if (user.getRole().equals(request.newRole())) {
            throw new IllegalArgumentException("У пользователя уже установлена выбранная роль");
        }
        user.setRole(request.newRole());
        userRepository.save(user);
        log.info("Роль пользователя {} изменена на {}", user.getUsername(), request.newRole());
    }

    public void deleteProfile(final UserDetailsImpl currentUser) {
        User user = findUserById(currentUser.getId());
        user.setIsEnabled(false);
        userRepository.save(user);
        log.info("Профиль пользователя {} удален", user.getUsername());
    }

    public void changeActive(final UUID id) {
        User user = findUserById(id);
        user.setIsEnabled(!user.getIsEnabled());
        userRepository.save(user);
        log.info("Изменен флаг активности пользователя {}", user.getUsername());
    }

    public void confirmEmail(final User user) {
        user.setIsEmailActive(true);
        user.setIsMailingAgree(true);
        userRepository.save(user);
        log.info("Пользователь {} подтвердил электронную почту {}", user.getUsername(), user.getEmail());
    }

    private void checkRightsForActionsWithUsers(final User user, final UUID id) {
        if (!user.getId().equals(id) && !user.getRole().equals(ERole.ADMIN)) {
            log.info("Попытка просмотра профиля другого пользователя со стороны {}", user.getUsername());
            throw new NoRightsException("У пользователя нет прав на просмотр выбранного профиля");
        }
    }
}