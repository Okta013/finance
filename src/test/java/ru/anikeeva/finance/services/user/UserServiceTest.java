package ru.anikeeva.finance.services.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Captor
    ArgumentCaptor<User> userCaptor;

    private static class TestUserData {
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();

        String firstUsername = "username";
        String secondUsername = "user";

        String firstEmail = "email@mail.ru";
        String secondEmail = "enail@mail.ru";

        String firstPassword = "password";
        String secondPassword = "pass";

        ERole userRole = ERole.USER;

        BigDecimal firstBalance = BigDecimal.valueOf(50000);
        BigDecimal secondBalance = BigDecimal.valueOf(150000);

        Currency firstBaseCurrency = Currency.getInstance("RUB");

        User firstUser = User.builder()
            .id(firstUserId)
            .username(firstUsername)
            .email(firstEmail)
            .password(firstPassword)
            .role(userRole)
            .balance(firstBalance)
            .baseCurrency(firstBaseCurrency)
            .isEnabled(true)
            .isEmailActive(false)
            .isMailingAgree(false)
            .build();

        User secondUser = User.builder()
            .id(secondUserId)
            .username(secondUsername)
            .email(secondEmail)
            .password(secondPassword)
            .role(userRole)
            .balance(secondBalance)
            .baseCurrency(firstBaseCurrency)
            .isEnabled(false)
            .isEmailActive(false)
            .isMailingAgree(false)
            .build();

        GrantedAuthority grantedAuthority = new SimpleGrantedAuthority("ROLE_USER");
        UserDetailsImpl currentUser = new UserDetailsImpl(firstUserId, firstUsername, firstPassword, grantedAuthority, true);
    }

    @Test
    @DisplayName("Поиск существующего пользователя по логину")
    public void findExistingUserByUsername() {
        TestUserData testUserData = new TestUserData();
        String username = "username";
        User expectedUser = testUserData.firstUser;

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(expectedUser));
        User actualUser = userService.findUserByUsername(username);

        assertEquals(expectedUser, actualUser);
    }

    @Test
    @DisplayName("Поиск несуществующего пользователя по логину")
    public void findNotExistingUserByUsername() {
        String username = "username";
        String expectedExceptionMessage = "Пользователь не найден";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        UsernameNotFoundException thrown = assertThrows(UsernameNotFoundException.class, () ->
            userService.findUserByUsername(username));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Поиск существующего пользователя по id")
    public void findExistingUserById() {
        TestUserData testUserData = new TestUserData();
        UUID id = testUserData.firstUserId;
        User expectedUser = testUserData.firstUser;

        when(userRepository.findById(id)).thenReturn(Optional.of(expectedUser));
        User actualUser = userService.findUserById(id);

        assertEquals(expectedUser, actualUser);
    }

    @Test
    @DisplayName("Поиск несуществующего пользователя по id")
    public void findNotExistingUserById() {
        UUID id = UUID.randomUUID();
        String expectedExceptionMessage = "Пользователь не найден";

        when(userRepository.findById(id)).thenReturn(Optional.empty());
        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () -> userService.findUserById(id));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Пересчет баланса пользователя")
    public void recalculateUserBalance() {
        TestUserData testUserData = new TestUserData();
        UUID id = testUserData.firstUserId;
        BigDecimal changes = BigDecimal.valueOf(1000);

        when(userRepository.updateBalance(id, changes)).thenReturn(1);
        userService.recalculateBalance(id, changes);

        verify(userRepository, times(1)).updateBalance(id, changes);
    }

    @Test
    @DisplayName("Чтение профиля пользователя")
    public void readUserProfile() {
        TestUserData testUserData = new TestUserData();
        User user = testUserData.firstUser;
        UserDetailsImpl currentUser = testUserData.currentUser;
        UUID id = testUserData.firstUserId;
        ReadUserResponse expectedResponse = new ReadUserResponse(testUserData.firstUsername, testUserData.firstEmail,
            testUserData.userRole.name(), testUserData.firstBalance, testUserData.firstBaseCurrency);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userMapper.toReadUserResponse(user)).thenReturn(expectedResponse);
        ReadUserResponse actualResponse = userService.readUserProfile(currentUser, id);

        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    @DisplayName("Чтение профиля другого пользователя")
    public void readAnotherUserProfile() {
        TestUserData testUserData = new TestUserData();
        User user = testUserData.firstUser;
        UserDetailsImpl currentUser = testUserData.currentUser;
        UUID id = UUID.randomUUID();
        String expectedExceptionMessage = "У пользователя нет прав на просмотр и изменение выбранного профиля";

        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(user));
        NoRightsException thrown = assertThrows(NoRightsException.class, () ->
            userService.readUserProfile(currentUser, id));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Чтение списка всех пользователей")
    void getAllUsersWithoutFilter() {
        TestUserData testUserData = new TestUserData();
        User firstUser = testUserData.firstUser;
        User secondUser = testUserData.secondUser;
        Page<User> userPage = new PageImpl<>(List.of(firstUser, secondUser));
        ReadUserListResponse firstUserResponse = new ReadUserListResponse(firstUser.getUsername(),
            firstUser.getRole().name(), firstUser.getIsEnabled());
        ReadUserListResponse secondUserResponse = new ReadUserListResponse(secondUser.getUsername(),
            secondUser.getRole().name(), secondUser.getIsEnabled());

        when(userRepository.findAll(any(PageRequest.class))).thenReturn(userPage);
        when(userMapper.toReadUserListResponse(firstUser)).thenReturn(firstUserResponse);
        when(userMapper.toReadUserListResponse(secondUser)).thenReturn(secondUserResponse);

        Page<ReadUserListResponse> result = userService.getAllUsers(0, 10, null);

        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().contains(firstUserResponse));
        assertTrue(result.getContent().contains(secondUserResponse));
    }

    @Test
    @DisplayName("Чтение списка всех активных пользователей")
    void getAllUsersWithFilterTrue() {
        TestUserData testUserData = new TestUserData();
        User firstUser = testUserData.firstUser;
        User secondUser = testUserData.secondUser;
        Page<User> userPage = new PageImpl<>(List.of(firstUser));
        Pageable pageable = PageRequest.of(0, 10);
        ReadUserListResponse firstUserResponse = new ReadUserListResponse(firstUser.getUsername(),
            firstUser.getRole().name(), firstUser.getIsEnabled());
        ReadUserListResponse secondUserResponse = new ReadUserListResponse(secondUser.getUsername(),
            secondUser.getRole().name(), secondUser.getIsEnabled());

        when(userRepository.findAllByIsEnabled(true, pageable)).thenReturn(userPage);
        when(userMapper.toReadUserListResponse(firstUser)).thenReturn(firstUserResponse);

        Page<ReadUserListResponse> result = userService.getAllUsers(0, 10, true);

        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().contains(firstUserResponse));
        assertFalse(result.getContent().contains(secondUserResponse));
    }

    @Test
    @DisplayName("Чтение списка всех неактивных пользователей")
    void getAllUsersWithFilterFalse() {
        TestUserData testUserData = new TestUserData();
        User firstUser = testUserData.firstUser;
        User secondUser = testUserData.secondUser;
        Page<User> userPage = new PageImpl<>(List.of(secondUser));
        Pageable pageable = PageRequest.of(0, 10);
        ReadUserListResponse firstUserResponse = new ReadUserListResponse(firstUser.getUsername(),
            firstUser.getRole().name(), firstUser.getIsEnabled());
        ReadUserListResponse secondUserResponse = new ReadUserListResponse(secondUser.getUsername(),
            secondUser.getRole().name(), secondUser.getIsEnabled());

        when(userRepository.findAllByIsEnabled(false, pageable)).thenReturn(userPage);
        when(userMapper.toReadUserListResponse(secondUser)).thenReturn(secondUserResponse);

        Page<ReadUserListResponse> result = userService.getAllUsers(0, 10, false);

        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().contains(secondUserResponse));
        assertFalse(result.getContent().contains(firstUserResponse));
    }

    @Test
    @DisplayName("Изменение профиля пользователя по корректному запросу")
    public void updateUserByCorrectRequest() {
        TestUserData testUserData = new TestUserData();
        User user = testUserData.firstUser;
        UUID id = testUserData.firstUserId;
        UserDetailsImpl currentUser = testUserData.currentUser;
        UpdateUserRequest request = new UpdateUserRequest("login", null, null);
        User updatedUser = User.builder()
            .id(id)
            .username(request.username())
            .email(testUserData.firstEmail)
            .password(testUserData.firstPassword)
            .role(testUserData.userRole)
            .balance(testUserData.firstBalance)
            .baseCurrency(testUserData.firstBaseCurrency)
            .isEnabled(true)
            .isEmailActive(false)
            .isMailingAgree(false)
            .build();

        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(user));
        doAnswer(answer -> {
            UpdateUserRequest req = answer.getArgument(0);
            User usr = answer.getArgument(1);
            usr.setUsername(req.username());
            return null;
        }).when(userMapper).updateUserFromUpdateUserRequest(any(UpdateUserRequest.class), any(User.class));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        userService.updateUser(currentUser, id, request);

        verify(userRepository).save(userCaptor.capture());
        assertEquals(updatedUser, userCaptor.getValue());
    }

    @Test
    @DisplayName("Изменение профиля пользователя по пустому запросу")
    public void updateUserByEmptyRequest() {
        TestUserData testUserData = new TestUserData();
        User user = testUserData.firstUser;
        UUID id = testUserData.firstUserId;
        UserDetailsImpl currentUser = testUserData.currentUser;
        UpdateUserRequest request = new UpdateUserRequest(null, null, null);
        String expectedExceptionMessage = "Запрос на изменение профиля пуст";

        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(user));
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
            userService.updateUser(currentUser, id, request));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Изменение профиля другого пользователя")
    public void updateAnotherUser() {
        TestUserData testUserData = new TestUserData();
        User user = testUserData.firstUser;
        UUID id = UUID.randomUUID();
        UserDetailsImpl currentUser = testUserData.currentUser;
        UpdateUserRequest request = new UpdateUserRequest("login", null, null);
        String expectedExceptionMessage = "У пользователя нет прав на просмотр и изменение выбранного профиля";

        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(user));
        NoRightsException thrown = assertThrows(NoRightsException.class, () ->
            userService.updateUser(currentUser, id, request));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Смена роли пользователя на новую")
    public void changeUserRoleToNew() {
        TestUserData testUserData = new TestUserData();
        User user = testUserData.firstUser;
        UUID id = testUserData.firstUserId;
        ChangeRoleRequest request = new ChangeRoleRequest(ERole.ADMIN);
        User updatedUser = User.builder()
            .id(id)
            .username(testUserData.firstUsername)
            .email(testUserData.firstEmail)
            .password(testUserData.firstPassword)
            .role(request.newRole())
            .balance(testUserData.firstBalance)
            .baseCurrency(testUserData.firstBaseCurrency)
            .isEnabled(true)
            .isEmailActive(false)
            .isMailingAgree(false)
            .build();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        userService.changeRole(id, request);

        verify(userRepository).save(userCaptor.capture());
        assertEquals(updatedUser, userCaptor.getValue());
    }

    @Test
    @DisplayName("Смена роли пользователя на уже установленную")
    public void changeUserRoleToAlreadyInstalled() {
        TestUserData testUserData = new TestUserData();
        User user = testUserData.firstUser;
        UUID id = testUserData.firstUserId;
        ChangeRoleRequest request = new ChangeRoleRequest(ERole.USER);
        String expectedExceptionMessage = "У пользователя уже установлена выбранная роль";

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
            userService.changeRole(id, request));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Удаление профиля пользователя")
    public void deleteUserProfile() {
        TestUserData testUserData = new TestUserData();
        User user = testUserData.firstUser;
        UserDetailsImpl currentUser = testUserData.currentUser;
        User deletedUser = User.builder()
            .id(testUserData.firstUserId)
            .username(testUserData.firstUsername)
            .email(testUserData.firstEmail)
            .password(testUserData.firstPassword)
            .role(testUserData.userRole)
            .balance(testUserData.firstBalance)
            .baseCurrency(testUserData.firstBaseCurrency)
            .isEnabled(false)
            .isEmailActive(false)
            .isMailingAgree(false)
            .build();

        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(deletedUser);
        userService.deleteProfile(currentUser);

        verify(userRepository).save(userCaptor.capture());
        assertEquals(deletedUser, userCaptor.getValue());
    }

    @Test
    @DisplayName("Изменение флага активности пользователя администратором")
    public void changeActiveByAdmin() {
        TestUserData testUserData = new TestUserData();
        User user = testUserData.firstUser;
        UUID id = testUserData.firstUserId;
        User updatedUser = User.builder()
            .id(testUserData.firstUserId)
            .username(testUserData.firstUsername)
            .email(testUserData.firstEmail)
            .password(testUserData.firstPassword)
            .role(testUserData.userRole)
            .balance(testUserData.firstBalance)
            .baseCurrency(testUserData.firstBaseCurrency)
            .isEnabled(false)
            .isEmailActive(false)
            .isMailingAgree(false)
            .build();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        userService.changeActive(id);

        verify(userRepository).save(userCaptor.capture());
        assertEquals(updatedUser, userCaptor.getValue());
    }

    @Test
    @DisplayName("Подтверждение электронной почты")
    public void confirmEmail() {
        TestUserData testUserData = new TestUserData();
        User user = testUserData.firstUser;
        User updatedUser = User.builder()
            .id(testUserData.firstUserId)
            .username(testUserData.firstUsername)
            .email(testUserData.firstEmail)
            .password(testUserData.firstPassword)
            .role(testUserData.userRole)
            .balance(testUserData.firstBalance)
            .baseCurrency(testUserData.firstBaseCurrency)
            .isEnabled(true)
            .isEmailActive(true)
            .isMailingAgree(true)
            .build();

        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        userService.confirmEmail(user);

        verify(userRepository).save(userCaptor.capture());
        assertEquals(updatedUser, userCaptor.getValue());
    }
}