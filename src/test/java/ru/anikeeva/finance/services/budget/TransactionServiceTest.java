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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import ru.anikeeva.finance.dto.budget.CreateTransactionRequest;
import ru.anikeeva.finance.dto.budget.CreateTransactionResponse;
import ru.anikeeva.finance.dto.budget.TransactionResponse;
import ru.anikeeva.finance.dto.budget.UpdateTransactionRequest;
import ru.anikeeva.finance.entities.budget.CurrencyRate;
import ru.anikeeva.finance.entities.budget.Transaction;
import ru.anikeeva.finance.entities.enums.ECurrencySource;
import ru.anikeeva.finance.entities.enums.ERole;
import ru.anikeeva.finance.entities.enums.ETransactionCategory;
import ru.anikeeva.finance.entities.enums.ETransactionType;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.exceptions.EmptyRequestException;
import ru.anikeeva.finance.exceptions.EntityNotFoundException;
import ru.anikeeva.finance.exceptions.InsufficientFundsException;
import ru.anikeeva.finance.exceptions.NoRightsException;
import ru.anikeeva.finance.mappers.TransactionMapper;
import ru.anikeeva.finance.repositories.budget.TransactionRepository;
import ru.anikeeva.finance.repositories.user.UserRepository;
import ru.anikeeva.finance.security.impl.UserDetailsImpl;
import ru.anikeeva.finance.services.user.UserService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {
    @InjectMocks
    private TransactionService transactionService;

    @Mock
    private UserService userService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrencyRateService currencyRateService;

    @Mock
    private BudgetService budgetService;

    private static class TestTransactionData {
        UUID firstTransactionId = UUID.randomUUID();
        UUID secondTransactionId = UUID.randomUUID();
        UUID thirdTransactionId = UUID.randomUUID();
        UUID fourthTransactionId = UUID.randomUUID();

        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        UUID thirdUserId = UUID.randomUUID();

        String firstUsername = "username";
        String secondUsername = "login";
        String thirdUsername = "user";

        String firstPassword = "password";
        String secondPassword = "pass";
        String thirdPassword = "passwd";

        Currency rubCurrency = Currency.getInstance("RUB");
        Currency usdCurrency = Currency.getInstance("USD");
        Currency eurCurrency = Currency.getInstance("EUR");

        User firstUser = User.builder()
            .id(firstUserId)
            .username(firstUsername)
            .password(firstPassword)
            .role(ERole.USER)
            .balance(BigDecimal.valueOf(50000))
            .baseCurrency(rubCurrency)
            .isEnabled(true)
            .isEmailActive(true)
            .isMailingAgree(true)
            .build();

        User secondUser = User.builder()
            .id(secondUserId)
            .username(secondUsername)
            .password(secondPassword)
            .role(ERole.USER)
            .balance(BigDecimal.valueOf(5000))
            .baseCurrency(rubCurrency)
            .isEnabled(true)
            .isEmailActive(true)
            .isMailingAgree(true)
            .build();

        User thirdUser = User.builder()
            .id(thirdUserId)
            .username(thirdUsername)
            .password(thirdPassword)
            .role(ERole.USER)
            .balance(BigDecimal.valueOf(5000))
            .baseCurrency(eurCurrency)
            .isEnabled(true)
            .isEmailActive(true)
            .isMailingAgree(true)
            .build();

        ETransactionType incomeType = ETransactionType.INCOME;
        ETransactionType expenseType = ETransactionType.EXPENSE;

        ETransactionCategory salaryCategory = ETransactionCategory.SALARY;
        ETransactionCategory foodCategory = ETransactionCategory.FOOD;
        ETransactionCategory pensionCategory = ETransactionCategory.PENSION;

        BigDecimal firstInitialAmount = BigDecimal.valueOf(50000);
        BigDecimal secondInitialAmount = BigDecimal.valueOf(1000);
        BigDecimal thirdInitialAmount = BigDecimal.valueOf(5000);
        BigDecimal fourthInitialAmount = BigDecimal.valueOf(25000);
        BigDecimal fifthInitialAmount = BigDecimal.valueOf(750);

        BigDecimal usdCurrencyRateAmount = BigDecimal.valueOf(83.61);
        BigDecimal eurCurrencyRateAmount = BigDecimal.valueOf(98.15);

        LocalDateTime currencyRateUpdateDate = LocalDateTime.of(2025, 9, 30, 0, 0, 0);

        CurrencyRate usdCurrencyRate = new CurrencyRate(UUID.randomUUID(), Currency.getInstance("USD"),
            "Доллар США", usdCurrencyRateAmount, currencyRateUpdateDate, ECurrencySource.CENTRAL_BANK);
        CurrencyRate eurCurrencyRate = new CurrencyRate(UUID.randomUUID(), Currency.getInstance("EUR"),
            "Евро", eurCurrencyRateAmount, currencyRateUpdateDate, ECurrencySource.CENTRAL_BANK);

        BigDecimal secondAmountInBaseCurrency = secondInitialAmount.multiply(usdCurrencyRateAmount);

        LocalDateTime firstTransactionDate = LocalDateTime.of(2025, 7, 1, 10, 15);
        LocalDateTime secondTransactionDate = LocalDateTime.of(2025, 8, 1, 10, 15);
        LocalDateTime thirdTransactionDate = LocalDateTime.of(2025, 9, 1, 10, 15);
        LocalDateTime fourthTransactionDate = LocalDateTime.of(2025, 9, 25, 10, 15);

        Transaction firstTransaction = Transaction.builder()
            .id(firstTransactionId)
            .user(firstUser)
            .type(incomeType)
            .category(salaryCategory)
            .initialAmount(firstInitialAmount)
            .initialCurrency(rubCurrency)
            .amountInBaseCurrency(firstInitialAmount)
            .dateTime(firstTransactionDate)
            .build();

        Transaction secondTransaction = Transaction.builder()
            .id(secondTransactionId)
            .user(secondUser)
            .type(incomeType)
            .category(salaryCategory)
            .initialAmount(secondInitialAmount)
            .initialCurrency(usdCurrency)
            .amountInBaseCurrency(secondAmountInBaseCurrency)
            .dateTime(secondTransactionDate)
            .build();

        Transaction thirdTransaction = Transaction.builder()
            .id(thirdTransactionId)
            .user(firstUser)
            .type(expenseType)
            .category(foodCategory)
            .initialAmount(thirdInitialAmount)
            .initialCurrency(rubCurrency)
            .amountInBaseCurrency(thirdInitialAmount)
            .dateTime(firstTransactionDate)
            .build();

        Transaction fourthTransaction = Transaction.builder()
            .id(fourthTransactionId)
            .user(firstUser)
            .type(incomeType)
            .category(pensionCategory)
            .initialAmount(fourthInitialAmount)
            .initialCurrency(rubCurrency)
            .amountInBaseCurrency(fourthInitialAmount)
            .dateTime(fourthTransactionDate)
            .build();

        GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(ERole.USER.name());
        UserDetailsImpl firstCurrentUser = new UserDetailsImpl(firstUserId, firstUsername, firstPassword,
            grantedAuthority, true);

        TransactionResponse firstTransactionResponse = new TransactionResponse(firstTransactionId, incomeType,
            salaryCategory, firstInitialAmount, rubCurrency, firstTransactionDate, null);
        TransactionResponse thirdTransactionResponse = new TransactionResponse(thirdTransactionId, expenseType,
            foodCategory, thirdInitialAmount, rubCurrency, thirdTransactionDate, null);
    }

    @Test
    @DisplayName("Создание транзакции при остатке лимита")
    public void createTransactionWithRemainingLimit() {
        TestTransactionData transactionData = new TestTransactionData();
        User user = transactionData.firstUser;
        UserDetailsImpl currentUser = transactionData.firstCurrentUser;
        ETransactionType type = ETransactionType.EXPENSE;
        ETransactionCategory category = ETransactionCategory.FOOD;
        BigDecimal initialAmount = BigDecimal.valueOf(5000);
        Currency initialCurrency = Currency.getInstance("RUB");
        LocalDateTime dateTime = LocalDateTime.of(2025, 8, 1, 10, 20);
        CreateTransactionRequest request = new CreateTransactionRequest(type, category, initialAmount, initialCurrency,
            dateTime, null);
        UUID transactionId = UUID.randomUUID();
        Transaction createdTransaction = Transaction.builder()
            .id(transactionId)
            .type(type)
            .category(category)
            .initialAmount(initialAmount)
            .initialCurrency(initialCurrency)
            .dateTime(dateTime)
            .build();
        CreateTransactionResponse expectedResponse = new CreateTransactionResponse(transactionId, true);

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        doNothing().when(budgetService).checkBudgetNotExceeded(user, category, initialAmount);
        when(transactionMapper.toTransaction(request)).thenReturn(createdTransaction);
        CreateTransactionResponse actualResponse = transactionService.createTransaction(currentUser, request);

        assertEquals(expectedResponse.isSuccess(), actualResponse.isSuccess());
        verify(transactionMapper).toTransaction(request);
        verify(transactionRepository).save(any(Transaction.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Создание транзакции при превышении баланса")
    public void createTransactionWhenBalanceExceeded() {
        TestTransactionData transactionData = new TestTransactionData();
        User user = transactionData.firstUser;
        UserDetailsImpl currentUser = transactionData.firstCurrentUser;
        ETransactionType type = ETransactionType.EXPENSE;
        ETransactionCategory category = ETransactionCategory.BEAUTY;
        BigDecimal initialAmount = BigDecimal.valueOf(60000);
        Currency initialCurrency = Currency.getInstance("RUB");
        LocalDateTime dateTime = LocalDateTime.of(2025, 8, 1, 10, 20);
        CreateTransactionRequest request = new CreateTransactionRequest(type, category, initialAmount, initialCurrency,
            dateTime, null);
        String expectedExceptionMessage = "Баланс пользователя меньше суммы транзакции";

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        InsufficientFundsException thrown = assertThrows(InsufficientFundsException.class, () ->
            transactionService.createTransaction(currentUser, request));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Чтение существующей транзакции")
    public void showExistingTransaction() {
        TestTransactionData transactionData = new TestTransactionData();
        UserDetailsImpl currentUser = transactionData.firstCurrentUser;
        UUID transactionId = transactionData.firstTransactionId;
        Transaction transaction = transactionData.firstTransaction;
        TransactionResponse expectedResponse = transactionData.firstTransactionResponse;

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionMapper.toTransactionResponse(transaction)).thenReturn(expectedResponse);
        TransactionResponse actualResponse = transactionService.showTransaction(currentUser, transactionId);

        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    @DisplayName("Чтение несуществующей транзакции")
    public void showNonExistingTransaction() {
        TestTransactionData transactionData = new TestTransactionData();
        UserDetailsImpl currentUser = transactionData.firstCurrentUser;
        UUID transactionId = UUID.randomUUID();
        String expectedExceptionMessage = "Транзакция не найдена";

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());
        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () ->
            transactionService.showTransaction(currentUser, transactionId));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Чтение транзакции другого пользователя")
    public void showTransactionOfAnotherUser() {
        TestTransactionData transactionData = new TestTransactionData();
        UserDetailsImpl currentUser = transactionData.firstCurrentUser;
        UUID transactionId = transactionData.secondTransactionId;
        String expectedExceptionMessage = "Транзакция не принадлежит текущему пользователю";
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transactionData.secondTransaction));
        NoRightsException thrown = assertThrows(NoRightsException.class, () ->
            transactionService.showTransaction(currentUser, transactionId));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Чтение списка транзакций пользователя")
    public void showAllTransactionsOfUser() {
        TestTransactionData transactionData = new TestTransactionData();
        UserDetailsImpl currentUser = transactionData.firstCurrentUser;

        List<Transaction> transactions = Arrays.asList(transactionData.firstTransaction,
            transactionData.thirdTransaction);
        Page<Transaction> transactionPage = new PageImpl<>(transactions, PageRequest.of(0, 10),
            transactions.size());

        when(transactionRepository.findAllByUserId(eq(transactionData.firstUserId), any(PageRequest.class)))
            .thenReturn(transactionPage);
        when(transactionMapper.toTransactionResponse(transactionData.firstTransaction))
            .thenReturn(transactionData.firstTransactionResponse);
        when(transactionMapper.toTransactionResponse(transactionData.thirdTransaction))
            .thenReturn(transactionData.thirdTransactionResponse);

        Page<TransactionResponse> result = transactionService.showAllTransactions(currentUser, 0, 10);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        List<TransactionResponse> responses = result.getContent();
        assertEquals(transactionData.firstTransactionResponse, responses.get(0));
        assertEquals(transactionData.thirdTransactionResponse, responses.get(1));

        verify(transactionRepository).findAllByUserId(eq(transactionData.firstUserId), any(PageRequest.class));
        verify(transactionMapper, times(2)).toTransactionResponse(any(Transaction.class));
    }

    @Test
    @DisplayName("Чтение пустого списка транзакций пользователя")
    public void showEmptyTransactionsListOfUser() {
        TestTransactionData transactionData = new TestTransactionData();
        UserDetailsImpl currentUser = transactionData.firstCurrentUser;

        Page<Transaction> emptyPage = new PageImpl<>(Collections.emptyList());
        when(transactionRepository.findAllByUserId(eq(transactionData.firstUserId), any(PageRequest.class)))
            .thenReturn(emptyPage);

        Page<TransactionResponse> result = transactionService.showAllTransactions(currentUser, 0, 10);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(transactionRepository).findAllByUserId(eq(transactionData.firstUserId), any(PageRequest.class));
        verifyNoInteractions(transactionMapper);
    }

    @Test
    @DisplayName("Чтение списка транзакций пользователя при передаче параметров пагинации")
    public void showAllTransactionsWithPaginationParameters() {
        TestTransactionData transactionData = new TestTransactionData();
        UserDetailsImpl currentUser = transactionData.firstCurrentUser;

        Page<Transaction> emptyPage = new PageImpl<>(Collections.emptyList());
        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);

        when(transactionRepository.findAllByUserId(eq(transactionData.firstUserId), any(PageRequest.class)))
            .thenReturn(emptyPage);

        transactionService.showAllTransactions(currentUser, 3, 15);

        verify(transactionRepository).findAllByUserId(eq(transactionData.firstUserId), pageRequestCaptor.capture());
        PageRequest captured = pageRequestCaptor.getValue();
        assertEquals(3, captured.getPageNumber());
        assertEquals(15, captured.getPageSize());
    }

    @Test
    @DisplayName("Успешное обновление транзакции")
    public void updateTransactionSuccess() {
        TestTransactionData transactionData = new TestTransactionData();
        UUID transactionId = transactionData.firstTransactionId;
        User user = transactionData.firstUser;
        UserDetailsImpl currentUser = transactionData.firstCurrentUser;
        BigDecimal newAmount = BigDecimal.valueOf(60000);
        UpdateTransactionRequest request = new UpdateTransactionRequest(null, null, newAmount,
            null, null, null);
        Transaction updatedTransaction = Transaction.builder()
            .id(transactionId)
            .user(user)
            .type(transactionData.incomeType)
            .category(transactionData.salaryCategory)
            .initialAmount(newAmount)
            .initialCurrency(transactionData.rubCurrency)
            .amountInBaseCurrency(newAmount)
            .dateTime(transactionData.firstTransactionDate)
            .build();
        TransactionResponse expectedResponse = new TransactionResponse(transactionData.firstTransactionId,
            transactionData.incomeType, transactionData.salaryCategory, newAmount, transactionData.rubCurrency,
            transactionData.firstTransactionDate, null);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transactionData.firstTransaction));
        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        doNothing().when(transactionMapper).updateTransactionFromUpdateTransactionRequest(request,
            transactionData.firstTransaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(updatedTransaction);
        when(transactionMapper.toTransactionResponse(updatedTransaction)).thenReturn(expectedResponse);

        TransactionResponse actualResponse = transactionService.updateTransaction(currentUser, transactionId, request);

        assertEquals(expectedResponse, actualResponse);
        verify(transactionMapper).updateTransactionFromUpdateTransactionRequest(request,
            transactionData.firstTransaction);
        verify(transactionRepository).save(updatedTransaction);
        verify(transactionMapper).toTransactionResponse(updatedTransaction);
    }

    @Test
    @DisplayName("Обновление транзакции с пустым запросом")
    public void updateTransactionEmptyRequestThrows() {
        TestTransactionData transactionData = new TestTransactionData();
        UUID transactionId = transactionData.firstTransactionId;
        UserDetailsImpl currentUser = transactionData.firstCurrentUser;
        UpdateTransactionRequest request = new UpdateTransactionRequest(null, null, null,
            null, null, null);
        String expectedExceptionMessage = "Запрос на изменение транзакции пустой";

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transactionData.firstTransaction));
        EmptyRequestException thrown = assertThrows(EmptyRequestException.class, () ->
            transactionService.updateTransaction(currentUser, transactionId, request));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
        verifyNoMoreInteractions(transactionMapper, transactionRepository);
    }

    @Test
    @DisplayName("Обновление несуществующей транзакции")
    public void updateNonExistingTransaction() {
        TestTransactionData transactionData = new TestTransactionData();
        UUID transactionId = UUID.randomUUID();
        UserDetailsImpl currentUser = transactionData.firstCurrentUser;
        UpdateTransactionRequest request = new UpdateTransactionRequest(null, null, BigDecimal.valueOf(50),
            null, null, null);
        String expectedExceptionMessage = "Транзакция не найдена";

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());
        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () ->
            transactionService.updateTransaction(currentUser, transactionId, request));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Обновление транзакции другого пользователя")
    public void updateTransactionOfAnotherUser() {
        TestTransactionData transactionData = new TestTransactionData();
        UUID transactionId = transactionData.secondTransactionId;
        UserDetailsImpl currentUser = transactionData.firstCurrentUser;
        UpdateTransactionRequest request = new UpdateTransactionRequest(null, null, BigDecimal.valueOf(50),
            null, null, null);
        String expectedExceptionMessage = "Транзакция не принадлежит текущему пользователю";

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transactionData.secondTransaction));
        NoRightsException thrown = assertThrows(NoRightsException.class, () ->
            transactionService.updateTransaction(currentUser, transactionId, request));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Удаление транзакции")
    public void deleteTransaction() {
        TestTransactionData transactionData = new TestTransactionData();
        UUID transactionId = transactionData.firstTransactionId;
        UserDetailsImpl currentUser = transactionData.firstCurrentUser;

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transactionData.firstTransaction));
        doNothing().when(transactionRepository).delete(transactionData.firstTransaction);
        transactionService.deleteTransaction(currentUser, transactionId);

        verify(transactionRepository).delete(transactionData.firstTransaction);
    }

    @Test
    @DisplayName("Удаление несуществующей транзакции")
    public void deleteNonExistingTransaction() {
        TestTransactionData transactionData = new TestTransactionData();
        UUID transactionId = transactionData.firstTransactionId;
        UserDetailsImpl currentUser = transactionData.firstCurrentUser;
        String expectedExceptionMessage = "Транзакция не найдена";

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());
        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () ->
            transactionService.deleteTransaction(currentUser, transactionId));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Удаление транзакции другого пользователя")
    public void deleteTransactionOfAnotherUser() {
        TestTransactionData transactionData = new TestTransactionData();
        UUID transactionId = transactionData.secondTransactionId;
        UserDetailsImpl currentUser = transactionData.firstCurrentUser;
        String expectedExceptionMessage = "Транзакция не принадлежит текущему пользователю";

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transactionData.secondTransaction));
        NoRightsException thrown = assertThrows(NoRightsException.class, () ->
            transactionService.deleteTransaction(currentUser, transactionId));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Получение общей суммы по типу транзакций")
    public void getAmountByTransactionType() {
        TestTransactionData transactionData = new TestTransactionData();
        User user = transactionData.firstUser;
        ETransactionType transactionType = transactionData.incomeType;
        LocalDateTime startDate = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 10, 1, 23, 59, 59);
        BigDecimal expectedAmount = BigDecimal.valueOf(75000);

        when(transactionRepository.findAllByUserIdAndTypeAndDateTimeBetween(user.getId(), transactionType, startDate,
            endDate)).thenReturn(Arrays.asList(transactionData.firstTransaction, transactionData.fourthTransaction));
        BigDecimal actualAmount = transactionService.getAmountByTransactionType(user, startDate, endDate,
            transactionType);

        assertEquals(expectedAmount, actualAmount);
    }

    @Test
    @DisplayName("Получение всех транзакций по типу")
    public void getAllTransactionsByType() {
        TestTransactionData transactionData = new TestTransactionData();
        User user = transactionData.firstUser;
        ETransactionType transactionType = transactionData.incomeType;
        LocalDateTime startDate = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 10, 1, 23, 59, 59);
        List<Transaction> expectedTransactions = Arrays.asList(transactionData.firstTransaction,
            transactionData.fourthTransaction);

        when(transactionRepository.findAllByUserIdAndTypeAndDateTimeBetween(user.getId(), transactionType, startDate,
            endDate)).thenReturn(expectedTransactions);
        List<Transaction> actualTransactions = transactionService.getAllTransactionsByType(user, startDate, endDate,
            transactionType);

        assertTrue(expectedTransactions.containsAll(actualTransactions));
    }

    @Test
    @DisplayName("Расчет суммы транзакции в базовой валюте рубль")
    public void calculateAmountWithRubBaseCurrency() {
        TestTransactionData transactionData = new TestTransactionData();
        User user = transactionData.secondUser;
        BigDecimal amount = transactionData.secondInitialAmount;
        Currency currency = transactionData.usdCurrency;
        CurrencyRate usdCurrencyRate = transactionData.usdCurrencyRate;
        BigDecimal expectedAmount = BigDecimal.valueOf(83610.00);

        when(currencyRateService.getCurrencyRateByCurrency(currency)).thenReturn(usdCurrencyRate);
        BigDecimal actualAmount = transactionService.calculateAmountWithBaseCurrency(user, amount, currency);

        assertTrue(expectedAmount.compareTo(actualAmount) == 0);
    }

    @Test
    @DisplayName("Расчет суммы транзакции в базовой валюте евро")
    public void calculateAmountWithEurBaseCurrency() {
        TestTransactionData transactionData = new TestTransactionData();
        User user = transactionData.thirdUser;
        BigDecimal amount = transactionData.fifthInitialAmount;
        Currency currency = transactionData.usdCurrency;
        CurrencyRate usdCurrencyRate = transactionData.usdCurrencyRate;
        CurrencyRate euroCurrencyRate = transactionData.eurCurrencyRate;
        BigDecimal expectedAmount = BigDecimal.valueOf(638.89);

        when(currencyRateService.getCurrencyRateByCurrency(currency)).thenReturn(usdCurrencyRate);
        when(currencyRateService.getCurrencyRateByCurrency(transactionData.eurCurrency)).thenReturn(euroCurrencyRate);
        BigDecimal actualAmount = transactionService.calculateAmountWithBaseCurrency(user, amount, currency);

        assertEquals(expectedAmount, actualAmount);
    }
}