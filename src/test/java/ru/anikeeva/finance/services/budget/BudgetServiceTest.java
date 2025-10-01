package ru.anikeeva.finance.services.budget;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import ru.anikeeva.finance.dto.budget.CreateBudgetRequest;
import ru.anikeeva.finance.dto.budget.CreateBudgetResponse;
import ru.anikeeva.finance.dto.budget.ReadBudgetResponse;
import ru.anikeeva.finance.dto.budget.UpdateBudgetRequest;
import ru.anikeeva.finance.dto.notifications.BudgetNotification;
import ru.anikeeva.finance.entities.budget.Budget;
import ru.anikeeva.finance.entities.budget.Transaction;
import ru.anikeeva.finance.entities.enums.EBudgetPeriod;
import ru.anikeeva.finance.entities.enums.ERole;
import ru.anikeeva.finance.entities.enums.ETransactionCategory;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.exceptions.BadDataException;
import ru.anikeeva.finance.exceptions.BudgetLimitExceedingException;
import ru.anikeeva.finance.exceptions.EntityNotFoundException;
import ru.anikeeva.finance.exceptions.NoRightsException;
import ru.anikeeva.finance.mappers.BudgetMapper;
import ru.anikeeva.finance.repositories.budget.BudgetRepository;
import ru.anikeeva.finance.repositories.budget.TransactionRepository;
import ru.anikeeva.finance.security.impl.UserDetailsImpl;
import ru.anikeeva.finance.services.user.UserService;
import ru.anikeeva.finance.services.websocket.WebSocketNotificationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BudgetServiceTest {
    @InjectMocks
    private BudgetService budgetService;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserService userService;

    @Mock
    private BudgetMapper budgetMapper;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WebSocketNotificationService notificationService;

    private static class TestBudgetData {
        UUID firstBudgetId = UUID.randomUUID();
        UUID secondBudgetId = UUID.randomUUID();
        UUID thirdBudgetId = UUID.randomUUID();
        UUID fourthBudgetId = UUID.randomUUID();
        UUID fifthBudgetId = UUID.randomUUID();

        BigDecimal firstBudgetLimitAmount = BigDecimal.valueOf(50000);
        BigDecimal secondBudgetLimitAmount = BigDecimal.valueOf(10000);
        BigDecimal thirdBudgetLimitAmount = BigDecimal.valueOf(5000);
        BigDecimal fourthBudgetLimitAmount = BigDecimal.valueOf(1000);
        BigDecimal fifthBudgetLimitAmount = BigDecimal.valueOf(5000);

        EBudgetPeriod firstBudgetPeriod = EBudgetPeriod.YEAR;
        EBudgetPeriod secondBudgetPeriod = EBudgetPeriod.MONTH;
        EBudgetPeriod thirdBudgetPeriod = EBudgetPeriod.WEEK;
        EBudgetPeriod fourthBudgetPeriod = EBudgetPeriod.DAY;

        ETransactionCategory firstBudgetCategory = ETransactionCategory.CAFE;
        ETransactionCategory secondBudgetCategory = ETransactionCategory.UTILITIES;
        ETransactionCategory thirdBudgetCategory = ETransactionCategory.FOOD;
        ETransactionCategory fourthBudgetCategory = ETransactionCategory.TRANSPORT;

        Budget firstBudgetWithoutUser = Budget.builder()
            .id(firstBudgetId)
            .limitAmount(firstBudgetLimitAmount)
            .period(firstBudgetPeriod)
            .category(firstBudgetCategory)
            .build();

        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();

        String firstUsername = "username";
        String secondUsername = "login";
        String firstPassword = "password";
        String secondPassword = "pass";

        User firstUser = User.builder()
            .id(firstUserId)
            .username(firstUsername)
            .password(firstPassword)
            .role(ERole.USER)
            .balance(BigDecimal.valueOf(50000))
            .baseCurrency(Currency.getInstance("RUB"))
            .isEnabled(true)
            .isEmailActive(true)
            .isMailingAgree(true)
            .build();

        User secondUser = User.builder()
            .id(secondUserId)
            .username(secondUsername)
            .password(secondPassword)
            .role(ERole.USER)
            .balance(BigDecimal.valueOf(10000))
            .baseCurrency(Currency.getInstance("RUB"))
            .isEnabled(true)
            .isEmailActive(true)
            .isMailingAgree(true)
            .build();

        Budget firstBudget = Budget.builder()
            .id(firstBudgetId)
            .user(firstUser)
            .limitAmount(firstBudgetLimitAmount)
            .period(firstBudgetPeriod)
            .category(firstBudgetCategory)
            .build();

        Budget secondBudget = Budget.builder()
            .id(secondBudgetId)
            .user(firstUser)
            .limitAmount(secondBudgetLimitAmount)
            .period(secondBudgetPeriod)
            .category(secondBudgetCategory)
            .build();

        Budget thirdBudget = Budget.builder()
            .id(thirdBudgetId)
            .user(firstUser)
            .limitAmount(thirdBudgetLimitAmount)
            .period(thirdBudgetPeriod)
            .category(thirdBudgetCategory)
            .build();

        Budget fourthBudget = Budget.builder()
            .id(fourthBudgetId)
            .user(secondUser)
            .limitAmount(fourthBudgetLimitAmount)
            .period(fourthBudgetPeriod)
            .category(fourthBudgetCategory)
            .build();

        Budget fifthBudget = Budget.builder()
            .id(fifthBudgetId)
            .user(firstUser)
            .limitAmount(fifthBudgetLimitAmount)
            .period(fourthBudgetPeriod)
            .category(secondBudgetCategory)
            .build();

        GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(ERole.USER.name());
        UserDetailsImpl currentUser = new UserDetailsImpl(firstUserId, firstUsername, firstPassword, grantedAuthority, true);
    }

    @Test
    @DisplayName("Создание бюджета по корректному запросу")
    public void createBudgetByCorrectRequest() {
        TestBudgetData budgetData = new TestBudgetData();
        User user = budgetData.firstUser;
        UserDetailsImpl currentUser = budgetData.currentUser;
        CreateBudgetRequest request = new CreateBudgetRequest(budgetData.firstBudgetLimitAmount, budgetData.firstBudgetPeriod,
            budgetData.firstBudgetCategory);
        CreateBudgetResponse expectedResponse = new CreateBudgetResponse(budgetData.firstBudgetId, true);

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        when(budgetRepository.existsByUserAndPeriodAndCategory(user, request.period(), request.category()))
            .thenReturn(false);
        when(budgetMapper.fromCreateBudgetRequest(request)).thenReturn(budgetData.firstBudgetWithoutUser);
        CreateBudgetResponse actualResponse = budgetService.createBudget(currentUser, request);
        ArgumentCaptor<Budget> budgetCaptor = ArgumentCaptor.forClass(Budget.class);

        assertEquals(expectedResponse, actualResponse);
        verify(budgetRepository).save(budgetCaptor.capture());
        assertEquals(budgetData.firstBudgetWithoutUser, budgetCaptor.getValue());
    }

    @Test
    @DisplayName("Повторное создание уже существующего бюджета")
    public void createAlreadyExistingBudget() {
        TestBudgetData budgetData = new TestBudgetData();
        User user = budgetData.firstUser;
        UserDetailsImpl currentUser = budgetData.currentUser;
        CreateBudgetRequest request = new CreateBudgetRequest(budgetData.firstBudgetLimitAmount, budgetData.firstBudgetPeriod,
            budgetData.firstBudgetCategory);
        String expectedExceptionMessage = "Бюджет по этой категории на выбранный период уже существует";

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        when(budgetRepository.existsByUserAndPeriodAndCategory(user, request.period(), request.category()))
            .thenReturn(true);
        BadDataException thrown = assertThrows(BadDataException.class, () ->
            budgetService.createBudget(currentUser, request));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Чтение бюджетов пользователя")
    public void getAllBudgetsOfUser() {
        TestBudgetData budgetData = new TestBudgetData();
        User user = budgetData.firstUser;
        UserDetailsImpl currentUser = budgetData.currentUser;
        Pageable pageable = PageRequest.of(0, 10);
        List<Budget> budgetList = List.of(budgetData.firstBudget, budgetData.secondBudget, budgetData.thirdBudget);
        Page<Budget> budgetPage = new PageImpl<>(budgetList, pageable, budgetList.size());
        ReadBudgetResponse firstBudgetResponse = new ReadBudgetResponse(budgetData.firstBudgetLimitAmount,
            budgetData.firstBudgetPeriod, budgetData.firstBudgetCategory);
        ReadBudgetResponse secondBudgetResponse = new ReadBudgetResponse(budgetData.secondBudgetLimitAmount,
            budgetData.secondBudgetPeriod, budgetData.secondBudgetCategory);
        ReadBudgetResponse thirdBudgetResponse = new ReadBudgetResponse(budgetData.thirdBudgetLimitAmount,
            budgetData.thirdBudgetPeriod, budgetData.thirdBudgetCategory);
        List<ReadBudgetResponse> readBudgetResponses = List.of(firstBudgetResponse, secondBudgetResponse,
            thirdBudgetResponse);
        Page<ReadBudgetResponse> expectedPage = new PageImpl<>(readBudgetResponses, pageable, readBudgetResponses.size());

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        when(budgetRepository.findAllByUser(user, pageable)).thenReturn(budgetPage);
        when(budgetMapper.fromBudget(budgetData.firstBudget)).thenReturn(firstBudgetResponse);
        when(budgetMapper.fromBudget(budgetData.secondBudget)).thenReturn(secondBudgetResponse);
        when(budgetMapper.fromBudget(budgetData.thirdBudget)).thenReturn(thirdBudgetResponse);
        Page<ReadBudgetResponse> actualPage = budgetService.getAllBudgets(currentUser, 0, 10);

        assertEquals(3, actualPage.getTotalElements());
        assertTrue(expectedPage.getContent().containsAll(actualPage.getContent()));
    }

    @Test
    @DisplayName("Чтение бюджетов пользователя при их отсутствии")
    public void getAllBudgetsOfUserWithoutBudgets() {
        TestBudgetData budgetData = new TestBudgetData();
        User user = budgetData.firstUser;
        UserDetailsImpl currentUser = budgetData.currentUser;
        Pageable pageable = PageRequest.of(0, 10);
        List<Budget> emptyBudgetList = Collections.emptyList();
        Page<Budget> budgetPage = new PageImpl<>(emptyBudgetList, pageable, 0);

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        when(budgetRepository.findAllByUser(user, pageable)).thenReturn(budgetPage);
        Page<ReadBudgetResponse> actualPage = budgetService.getAllBudgets(currentUser, 0, 10);

        assertEquals(0, actualPage.getTotalElements());
        assertTrue(actualPage.getContent().isEmpty());
    }

    @Test
    @DisplayName("Чтение информации о бюджете по корректному запросу")
    public void getBudgetByCorrectRequest() {
        TestBudgetData budgetData = new TestBudgetData();
        User user = budgetData.firstUser;
        UserDetailsImpl currentUser = budgetData.currentUser;
        ReadBudgetResponse expectedResponse = new ReadBudgetResponse(budgetData.firstBudgetLimitAmount,
            budgetData.firstBudgetPeriod, budgetData.firstBudgetCategory);

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        when(budgetRepository.findById(budgetData.firstBudgetId)).thenReturn(Optional.ofNullable(budgetData.firstBudget));
        when(budgetMapper.fromBudget(budgetData.firstBudget)).thenReturn(expectedResponse);
        ReadBudgetResponse actualResponse = budgetService.getBudget(currentUser, budgetData.firstBudgetId);

        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    @DisplayName("Чтение информации о бюджете другого пользователя")
    public void getBudgetOfAnotherUser() {
        TestBudgetData budgetData = new TestBudgetData();
        User user = budgetData.firstUser;
        UserDetailsImpl currentUser = budgetData.currentUser;
        String expectedExceptionMessage = "У пользователя нет прав на просмотр выбранного бюджета";

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        when(budgetRepository.findById(budgetData.fourthBudgetId))
            .thenReturn(Optional.ofNullable(budgetData.fourthBudget));
        NoRightsException thrown = assertThrows(NoRightsException.class, () ->
            budgetService.getBudget(currentUser, budgetData.fourthBudgetId));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Чтение несуществующего бюджета")
    public void getNonExistingBudget() {
        TestBudgetData budgetData = new TestBudgetData();
        User user = budgetData.firstUser;
        UserDetailsImpl currentUser = budgetData.currentUser;
        String expectedExceptionMessage = "Бюджет не найден";

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () ->
            budgetService.getBudget(currentUser, UUID.randomUUID()));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Изменение бюджета по корректному запросу")
    public void updateBudgetByCorrectRequest() {
        TestBudgetData budgetData = new TestBudgetData();
        User user = budgetData.firstUser;
        UserDetailsImpl currentUser = budgetData.currentUser;
        UpdateBudgetRequest request = new UpdateBudgetRequest(null, EBudgetPeriod.WEEK, null);
        Budget updatedFirstBudget = Budget.builder()
            .id(budgetData.firstBudgetId)
            .user(user)
            .limitAmount(budgetData.firstBudgetLimitAmount)
            .period(budgetData.thirdBudgetPeriod)
            .category(budgetData.firstBudgetCategory)
            .build();
        ReadBudgetResponse expectedResponse = new ReadBudgetResponse(updatedFirstBudget.getLimitAmount(),
            updatedFirstBudget.getPeriod(), updatedFirstBudget.getCategory());

        when(budgetRepository.findById(budgetData.firstBudgetId)).thenReturn(Optional.ofNullable(budgetData.firstBudget));
        doAnswer(answer -> {
            UpdateBudgetRequest updateBudgetRequest = answer.getArgument(0);
            Budget budget = answer.getArgument(1);
            budget.setPeriod(updatedFirstBudget.getPeriod());
            return null;
        }).when(budgetMapper).updateBudgetFromUpdateBudgetRequest(request, budgetData.firstBudget);
        when(budgetMapper.fromBudget(updatedFirstBudget)).thenReturn(expectedResponse);
        ReadBudgetResponse actualResponse = budgetService.updateBudget(currentUser, budgetData.firstBudgetId, request);

        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    @DisplayName("Изменение бюджета по пустому запросу")
    public void updateBudgetByEmptyRequest() {
        TestBudgetData budgetData = new TestBudgetData();
        UserDetailsImpl currentUser = budgetData.currentUser;
        UpdateBudgetRequest request = new UpdateBudgetRequest(null, null, null);
        String expectedExceptionMessage = "Запрос на изменение бюджета пуст";

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
            budgetService.updateBudget(currentUser, budgetData.firstBudgetId, request));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Изменение бюджета другого пользователя")
    public void updateBudgetOfAnotherUser() {
        TestBudgetData budgetData = new TestBudgetData();
        UserDetailsImpl currentUser = budgetData.currentUser;
        UpdateBudgetRequest request = new UpdateBudgetRequest(null, EBudgetPeriod.WEEK, null);
        String expectedExceptionMessage = "У пользователя нет прав на изменение выбранного бюджета";

        when(budgetRepository.findById(budgetData.fourthBudgetId))
            .thenReturn(Optional.ofNullable(budgetData.fourthBudget));
        NoRightsException thrown = assertThrows(NoRightsException.class, () ->
            budgetService.updateBudget(currentUser, budgetData.fourthBudgetId, request));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Удаление бюджета")
    public void deleteBudget() {
        TestBudgetData budgetData = new TestBudgetData();
        User user = budgetData.firstUser;
        UserDetailsImpl currentUser = budgetData.currentUser;

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        when(budgetRepository.findById(budgetData.firstBudgetId))
            .thenReturn(Optional.ofNullable(budgetData.firstBudget));
        budgetService.deleteBudget(currentUser, budgetData.firstBudgetId);

        verify(budgetRepository).delete(budgetData.firstBudget);
    }

    @Test
    @DisplayName("Удаление бюджета другого пользователя")
    public void deleteBudgetOfAnotherUser() {
        TestBudgetData budgetData = new TestBudgetData();
        User user = budgetData.firstUser;
        UserDetailsImpl currentUser = budgetData.currentUser;
        String expectedExceptionMessage = "У пользователя нет прав на удаление выбранного бюджета";

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        when(budgetRepository.findById(budgetData.fourthBudgetId))
            .thenReturn(Optional.ofNullable(budgetData.fourthBudget));
        NoRightsException thrown = assertThrows(NoRightsException.class, () ->
            budgetService.deleteBudget(currentUser, budgetData.fourthBudgetId));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Проверка превышения бюджетов при остатке лимита")
    public void checkBudgetNotExceededWhenLimitRemains() {
        TestBudgetData budgetData = new TestBudgetData();
        User user = budgetData.firstUser;
        ETransactionCategory category = budgetData.firstBudgetCategory;
        BigDecimal amount = BigDecimal.valueOf(500);

        when(budgetRepository.findAllByUserAndCategory(user, category)).thenReturn(List.of(budgetData.firstBudget,
            budgetData.fifthBudget));
        when(transactionRepository.findAllByUserAndCategoryAndDateTimeBetween(eq(user), eq(category), any(), any()))
            .thenAnswer(invocation -> {
                LocalDateTime start = invocation.getArgument(2);
                if (start.toLocalDate().equals(LocalDate.now())) {
                    Transaction t = new Transaction();
                    t.setInitialAmount(BigDecimal.valueOf(700));
                    return List.of(t);
                } else {
                    Transaction t = new Transaction();
                    t.setInitialAmount(BigDecimal.valueOf(1000));
                    return List.of(t);
                }
            });
        budgetService.checkBudgetNotExceeded(user, category, amount);

        verify(notificationService, times(0)).sendBudgetWarning(anyString(), any());
    }

    @Test
    @DisplayName("Проверка превышения бюджетов при исчерпании лимита")
    public void checkBudgetExceededWhenLimitReached() {
        TestBudgetData budgetData = new TestBudgetData();
        User user = budgetData.firstUser;
        ETransactionCategory category = budgetData.firstBudgetCategory;
        BigDecimal amount = BigDecimal.valueOf(5000);
        String expectedExceptionMessage = "Превышен дневной лимит расходов по категории, доступный остаток 4300,00 руб.";

        when(budgetRepository.findAllByUserAndCategory(user, category)).thenReturn(List.of(budgetData.firstBudget,
            budgetData.fifthBudget));
        when(transactionRepository.findAllByUserAndCategoryAndDateTimeBetween(eq(user), eq(category), any(), any()))
            .thenAnswer(invocation -> {
                LocalDateTime start = invocation.getArgument(2);
                if (start.toLocalDate().equals(LocalDate.now())) {
                    Transaction t = new Transaction();
                    t.setInitialAmount(BigDecimal.valueOf(700));
                    return List.of(t);
                } else {
                    Transaction t = new Transaction();
                    t.setInitialAmount(BigDecimal.valueOf(1000));
                    return List.of(t);
                }
            });
        BudgetLimitExceedingException thrown = assertThrows(BudgetLimitExceedingException.class, () ->
            budgetService.checkBudgetNotExceeded(user, category, amount));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
        verify(notificationService).sendBudgetWarning(anyString(), any());
    }

    @Test
    @DisplayName("Проверка превышения бюджета при прохождении порога лимита")
    public void checkBudgetExceededWhenPassingLimitThreshold() {
        TestBudgetData budgetData = new TestBudgetData();
        User user = budgetData.firstUser;
        ETransactionCategory category = budgetData.firstBudgetCategory;
        BigDecimal amount = BigDecimal.valueOf(500);
        String expectedExceptionMessage = "Превышен дневной лимит расходов по категории, доступный остаток 4300,00 руб.";

        when(budgetRepository.findAllByUserAndCategory(user, category)).thenReturn(List.of(budgetData.firstBudget,
            budgetData.fifthBudget));
        when(transactionRepository.findAllByUserAndCategoryAndDateTimeBetween(eq(user), eq(category), any(), any()))
            .thenAnswer(invocation -> {
                LocalDateTime start = invocation.getArgument(2);
                if (start.toLocalDate().equals(LocalDate.now())) {
                    Transaction t = new Transaction();
                    t.setInitialAmount(BigDecimal.valueOf(3700));
                    return List.of(t);
                } else {
                    Transaction t = new Transaction();
                    t.setInitialAmount(BigDecimal.valueOf(1000));
                    return List.of(t);
                }
            });
        budgetService.checkBudgetNotExceeded(user, category, amount);

        verify(notificationService, atLeastOnce()).sendBudgetWarning(eq(user.getId().toString()),
            any(BudgetNotification.class));
    }
}