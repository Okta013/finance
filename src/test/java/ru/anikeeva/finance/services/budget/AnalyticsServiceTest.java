package ru.anikeeva.finance.services.budget;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import ru.anikeeva.finance.dto.analytics.AnalyticsCategoriesResponse;
import ru.anikeeva.finance.dto.analytics.AnalyticsCategoryResponse;
import ru.anikeeva.finance.dto.analytics.AnalyticsMetricsRequest;
import ru.anikeeva.finance.dto.analytics.AnalyticsMetricsResponse;
import ru.anikeeva.finance.dto.analytics.AnalyticsTransactionsResponse;
import ru.anikeeva.finance.dto.analytics.CategoriesDiffResponse;
import ru.anikeeva.finance.entities.budget.Transaction;
import ru.anikeeva.finance.entities.enums.ERole;
import ru.anikeeva.finance.entities.enums.ETransactionCategory;
import ru.anikeeva.finance.entities.enums.ETransactionType;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.security.impl.UserDetailsImpl;
import ru.anikeeva.finance.services.user.UserService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {
    @InjectMocks
    private AnalyticsService analyticsService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserService userService;

    private static class TestAnalyticsData {
        UUID firstTransactionId = UUID.randomUUID();
        UUID secondTransactionId = UUID.randomUUID();
        UUID thirdTransactionId = UUID.randomUUID();
        UUID fourthTransactionId = UUID.randomUUID();
        UUID fifthTransactionId = UUID.randomUUID();
        UUID sixthTransactionId = UUID.randomUUID();
        UUID seventhTransactionId = UUID.randomUUID();
        UUID eighthTransactionId = UUID.randomUUID();

        UUID userId = UUID.randomUUID();
        String username = "username";
        String password = "password";

        User user = User.builder()
            .id(userId)
            .username(username)
            .password(password)
            .role(ERole.USER)
            .balance(BigDecimal.valueOf(50000))
            .baseCurrency(Currency.getInstance("RUB"))
            .isEnabled(true)
            .isEmailActive(true)
            .isMailingAgree(true)
            .build();

        ETransactionType incomeType = ETransactionType.INCOME;
        ETransactionType expenseType = ETransactionType.EXPENSE;

        ETransactionCategory salaryCategory = ETransactionCategory.SALARY;
        ETransactionCategory rentCategory = ETransactionCategory.RENT;
        ETransactionCategory foodCategory = ETransactionCategory.FOOD;
        ETransactionCategory taxesCategory = ETransactionCategory.TAXES;
        ETransactionCategory giftsCategory = ETransactionCategory.GIFTS;
        ETransactionCategory healthCategory = ETransactionCategory.HEALTH;

        BigDecimal initialAmountForFirstCategory = BigDecimal.valueOf(50000);
        BigDecimal initialAmountForSecondCategory = BigDecimal.valueOf(300);
        BigDecimal initialAmountForThirdCategory = BigDecimal.valueOf(10000);
        BigDecimal initialAmountForFourthCategory = BigDecimal.valueOf(200);
        BigDecimal initialAmountForFifthCategory = BigDecimal.valueOf(50000);
        BigDecimal initialAmountForSixthCategory = BigDecimal.valueOf(400);
        BigDecimal initialAmountForSeventhCategory = BigDecimal.valueOf(200);
        BigDecimal initialAmountForEighthCategory = BigDecimal.valueOf(40000);

        Currency firstInitialCurrency = Currency.getInstance("RUB");
        Currency secondInitialCurrency = Currency.getInstance("USD");

        BigDecimal currencyRate = BigDecimal.valueOf(83.61);

        BigDecimal secondAmountInBaseCurrency = initialAmountForSecondCategory.multiply(currencyRate);
        BigDecimal fourthAmountInBaseCurrency = initialAmountForFourthCategory.multiply(currencyRate);
        BigDecimal sixthAmountInBaseCurrency = initialAmountForSixthCategory.multiply(currencyRate);
        BigDecimal seventhAmountInBaseCurrency = initialAmountForSeventhCategory.multiply(currencyRate);

        LocalDateTime firstTransactionDate = LocalDateTime.of(2025, 7, 1, 10, 15);
        LocalDateTime secondTransactionDate = LocalDateTime.of(2025, 7, 10, 14, 30);
        LocalDateTime thirdTransactionDate = LocalDateTime.of(2025, 7, 15, 9, 0);
        LocalDateTime fourthTransactionDate = LocalDateTime.of(2025, 7, 25, 18, 45);
        LocalDateTime fifthTransactionDate = LocalDateTime.of(2025, 8, 1, 10, 15);
        LocalDateTime sixthTransactionDate = LocalDateTime.of(2025, 8, 10, 14, 30);
        LocalDateTime seventhTransactionDate = LocalDateTime.of(2025, 8, 15, 9, 0);
        LocalDateTime eighthTransactionDate = LocalDateTime.of(2025, 8, 25, 18, 45);

        String firstTransactionDescription = "First transaction";
        String secondTransactionDescription = "Second transaction";
        String thirdTransactionDescription = "Third transaction";
        String fourthTransactionDescription = "Fourth transaction";
        String fifthTransactionDescription = "Fifth transaction";
        String sixthTransactionDescription = "Sixth transaction";
        String seventhTransactionDescription = "Seventh transaction";
        String eighthTransactionDescription = "Eighth transaction";

        Transaction firstTransaction = Transaction.builder()
            .id(firstTransactionId)
            .user(user)
            .type(incomeType)
            .category(salaryCategory)
            .initialAmount(initialAmountForFirstCategory)
            .initialCurrency(firstInitialCurrency)
            .amountInBaseCurrency(initialAmountForFirstCategory)
            .dateTime(firstTransactionDate)
            .description(firstTransactionDescription)
            .build();

        Transaction secondTransaction = Transaction.builder()
            .id(secondTransactionId)
            .user(user)
            .type(incomeType)
            .category(rentCategory)
            .initialAmount(initialAmountForSecondCategory)
            .initialCurrency(secondInitialCurrency)
            .amountInBaseCurrency(secondAmountInBaseCurrency)
            .dateTime(secondTransactionDate)
            .description(secondTransactionDescription)
            .build();

        Transaction thirdTransaction = Transaction.builder()
            .id(thirdTransactionId)
            .user(user)
            .type(expenseType)
            .category(foodCategory)
            .initialAmount(initialAmountForThirdCategory)
            .initialCurrency(firstInitialCurrency)
            .amountInBaseCurrency(initialAmountForThirdCategory)
            .dateTime(thirdTransactionDate)
            .description(thirdTransactionDescription)
            .build();

        Transaction fourthTransaction = Transaction.builder()
            .id(fourthTransactionId)
            .user(user)
            .type(expenseType)
            .category(taxesCategory)
            .initialAmount(initialAmountForFourthCategory)
            .initialCurrency(secondInitialCurrency)
            .amountInBaseCurrency(fourthAmountInBaseCurrency)
            .dateTime(fourthTransactionDate)
            .description(fourthTransactionDescription)
            .build();

        Transaction fifthTransaction = Transaction.builder()
            .id(fifthTransactionId)
            .user(user)
            .type(incomeType)
            .category(salaryCategory)
            .initialAmount(initialAmountForFifthCategory)
            .initialCurrency(firstInitialCurrency)
            .amountInBaseCurrency(initialAmountForFifthCategory)
            .dateTime(fifthTransactionDate)
            .description(fifthTransactionDescription)
            .build();

        Transaction sixthTransaction = Transaction.builder()
            .id(sixthTransactionId)
            .user(user)
            .type(incomeType)
            .category(giftsCategory)
            .initialAmount(initialAmountForSixthCategory)
            .initialCurrency(secondInitialCurrency)
            .amountInBaseCurrency(sixthAmountInBaseCurrency)
            .dateTime(sixthTransactionDate)
            .description(sixthTransactionDescription)
            .build();

        Transaction seventhTransaction = Transaction.builder()
            .id(seventhTransactionId)
            .user(user)
            .type(expenseType)
            .category(taxesCategory)
            .initialAmount(initialAmountForSeventhCategory)
            .initialCurrency(secondInitialCurrency)
            .amountInBaseCurrency(seventhAmountInBaseCurrency)
            .dateTime(seventhTransactionDate)
            .description(seventhTransactionDescription)
            .build();

        Transaction eighthTransaction = Transaction.builder()
            .id(eighthTransactionId)
            .user(user)
            .type(expenseType)
            .category(healthCategory)
            .initialAmount(initialAmountForEighthCategory)
            .initialCurrency(firstInitialCurrency)
            .amountInBaseCurrency(initialAmountForEighthCategory)
            .dateTime(eighthTransactionDate)
            .description(eighthTransactionDescription)
            .build();

        GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(ERole.USER.name());
        UserDetailsImpl currentUser = new UserDetailsImpl(userId, username, password, grantedAuthority, true);

        DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        String startDateInStr = "2025-07-01T00:00:00";
        String endDateInStr = "2025-07-31T23:59:59";
        LocalDateTime startDate = LocalDateTime.parse(startDateInStr, FORMATTER);
        LocalDateTime endDate = LocalDateTime.parse(endDateInStr, FORMATTER);
        String incorrectDateInStr = "2025-07-31 23:59:59";
        String secondStartDateInStr = "2025-08-01T00:00:00";
        String secondEndDateInStr = "2025-08-31T23:59:59";
        LocalDateTime secondStartDate = LocalDateTime.parse(secondStartDateInStr, FORMATTER);
        LocalDateTime secondEndDate = LocalDateTime.parse(secondEndDateInStr, FORMATTER);
    }

    @Test
    @DisplayName("Запрос аналитики сумм транзакций за период с верными датами")
    public void getAnalyticsTransactionsWithCorrectDates() {
        TestAnalyticsData analyticsData = new TestAnalyticsData();
        User user = analyticsData.user;
        UserDetailsImpl currentUser = analyticsData.currentUser;
        String startDateInStr = analyticsData.startDateInStr;
        String endDateInStr = analyticsData.endDateInStr;
        LocalDateTime startDate = analyticsData.startDate;
        LocalDateTime endDate = analyticsData.endDate;
        BigDecimal incomeAmount = analyticsData.firstTransaction.getAmountInBaseCurrency()
            .add(analyticsData.secondTransaction.getAmountInBaseCurrency());
        BigDecimal expenseAmount = analyticsData.thirdTransaction.getAmountInBaseCurrency()
            .add(analyticsData.fourthTransaction.getAmountInBaseCurrency());
        AnalyticsTransactionsResponse expectedResponse = new AnalyticsTransactionsResponse(incomeAmount, expenseAmount);

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        when(transactionService.getAmountByTransactionType(user, startDate, endDate, ETransactionType.INCOME))
            .thenReturn(incomeAmount);
        when(transactionService.getAmountByTransactionType(user, startDate, endDate, ETransactionType.EXPENSE))
            .thenReturn(expenseAmount);
        AnalyticsTransactionsResponse actualResponse = analyticsService.getAnalyticsTransactions(currentUser,
            startDateInStr, endDateInStr);

        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    @DisplayName("Запрос аналитики сумм транзакций за период с неверными датами")
    public void getAnalyticsTransactionsWithIncorrectDates() {
        TestAnalyticsData analyticsData = new TestAnalyticsData();
        User user = analyticsData.user;
        UserDetailsImpl currentUser = analyticsData.currentUser;
        String startDateInStr = analyticsData.endDateInStr;
        String endDateInStr = analyticsData.startDateInStr;
        String expectedExceptionMessage = "Дата начала периода не должна быть позже даты окончания";

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
            analyticsService.getAnalyticsTransactions(currentUser, startDateInStr, endDateInStr));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Запрос аналитики сумм транзакций за период с некорректным форматом дат")
    public void getAnalyticsTransactionsWithIncorrectDateFormat() {
        TestAnalyticsData analyticsData = new TestAnalyticsData();
        User user = analyticsData.user;
        UserDetailsImpl currentUser = analyticsData.currentUser;
        String startDateInStr = analyticsData.startDateInStr;
        String incorrectEndDateInStr = analyticsData.incorrectDateInStr;
        String expectedExceptionMessage = "Неверный формат даты (ожидается yyyy-MM-dd'T'HH:mm:ss): "
            + incorrectEndDateInStr;

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
            analyticsService.getAnalyticsTransactions(currentUser, startDateInStr, incorrectEndDateInStr));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Запрос аналитики доходов по категориям за период с верными датами")
    public void getAnalyticsByIncomeCategoriesWithCorrectDates() {
        TestAnalyticsData analyticsData = new TestAnalyticsData();
        User user = analyticsData.user;
        UserDetailsImpl currentUser = analyticsData.currentUser;
        String startDateInStr = analyticsData.startDateInStr;
        String endDateInStr = analyticsData.endDateInStr;
        LocalDateTime startDate = analyticsData.startDate;
        LocalDateTime endDate = analyticsData.endDate;
        String transactionType = "income";
        List<Transaction> incomeTransactions = List.of(analyticsData.firstTransaction, analyticsData.secondTransaction);
        AnalyticsCategoryResponse firstCategoryResponse = new AnalyticsCategoryResponse("SALARY", 66.59);
        AnalyticsCategoryResponse secondCategoryResponse = new AnalyticsCategoryResponse("RENT", 33.41);
        List<AnalyticsCategoryResponse> analyticsCategoryResponses = new ArrayList<>();
        analyticsCategoryResponses.add(firstCategoryResponse);
        analyticsCategoryResponses.add(secondCategoryResponse);
        AnalyticsCategoriesResponse expectedResponse = new AnalyticsCategoriesResponse(analyticsCategoryResponses);

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        when(transactionService.getAllTransactionsByType(user, startDate, endDate, ETransactionType.INCOME))
            .thenReturn(incomeTransactions);
        AnalyticsCategoriesResponse actualResponse = analyticsService.getAnalyticsByCategories(currentUser,
            startDateInStr, endDateInStr, transactionType);

        assertTrue(actualResponse.responses().containsAll(expectedResponse.responses())
            && expectedResponse.responses().containsAll(actualResponse.responses()));
    }

    @Test
    @DisplayName("Запрос аналитики расходов по категориям за период с верными датами")
    public void getAnalyticsByExpenseCategoriesWithCorrectDates() {
        TestAnalyticsData analyticsData = new TestAnalyticsData();
        User user = analyticsData.user;
        UserDetailsImpl currentUser = analyticsData.currentUser;
        String startDateInStr = analyticsData.startDateInStr;
        String endDateInStr = analyticsData.endDateInStr;
        LocalDateTime startDate = analyticsData.startDate;
        LocalDateTime endDate = analyticsData.endDate;
        String transactionType = "expense";
        List<Transaction> expenseTransactions = List.of(analyticsData.thirdTransaction, analyticsData.fourthTransaction);
        AnalyticsCategoryResponse thirdCategoryResponse = new AnalyticsCategoryResponse("FOOD", 37.42);
        AnalyticsCategoryResponse fourthCategoryResponse = new AnalyticsCategoryResponse("TAXES", 62.58);
        List<AnalyticsCategoryResponse> analyticsCategoryResponses = new ArrayList<>();
        analyticsCategoryResponses.add(thirdCategoryResponse);
        analyticsCategoryResponses.add(fourthCategoryResponse);
        AnalyticsCategoriesResponse expectedResponse = new AnalyticsCategoriesResponse(analyticsCategoryResponses);

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        when(transactionService.getAllTransactionsByType(user, startDate, endDate, ETransactionType.EXPENSE))
            .thenReturn(expenseTransactions);
        AnalyticsCategoriesResponse actualResponse = analyticsService.getAnalyticsByCategories(currentUser,
            startDateInStr, endDateInStr, transactionType);

        assertTrue(actualResponse.responses().containsAll(expectedResponse.responses())
            && expectedResponse.responses().containsAll(actualResponse.responses()));
    }

    @Test
    @DisplayName("Запрос аналитики по категориям за период с неверными датами")
    public void getAnalyticsByIncomeCategoriesWithIncorrectDates() {
        TestAnalyticsData analyticsData = new TestAnalyticsData();
        User user = analyticsData.user;
        UserDetailsImpl currentUser = analyticsData.currentUser;
        String startDateInStr = analyticsData.endDateInStr;
        String endDateInStr = analyticsData.startDateInStr;
        String expectedExceptionMessage = "Дата начала периода не должна быть позже даты окончания";

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
            analyticsService.getAnalyticsByCategories(currentUser, startDateInStr, endDateInStr, "INCOME"));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Запрос аналитики по категориям за период с неверным форматом даты")
    public void getAnalyticsByIncomeCategoriesWithIncorrectDateFormat() {
        TestAnalyticsData analyticsData = new TestAnalyticsData();
        User user = analyticsData.user;
        UserDetailsImpl currentUser = analyticsData.currentUser;
        String startDateInStr = analyticsData.startDateInStr;
        String incorrectEndDateInStr = analyticsData.incorrectDateInStr;
        String expectedExceptionMessage = "Неверный формат даты (ожидается yyyy-MM-dd'T'HH:mm:ss): "
            + incorrectEndDateInStr;

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
            analyticsService.getAnalyticsByCategories(currentUser, startDateInStr, incorrectEndDateInStr, "INCOME"));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Запрос аналитики для сравнения двух периодов с корректными датами")
    public void getAnalyticsByMetricsWithCorrectDates() {
        TestAnalyticsData analyticsData = new TestAnalyticsData();
        User user = analyticsData.user;
        UserDetailsImpl currentUser = analyticsData.currentUser;
        LocalDateTime startDateFirst = analyticsData.startDate;
        LocalDateTime endDateFirst = analyticsData.endDate;
        LocalDateTime startDateSecond = analyticsData.secondStartDate;
        LocalDateTime endDateSecond = analyticsData.secondEndDate;
        List<Transaction> firstIncomeTransactions = List.of(analyticsData.firstTransaction,
            analyticsData.secondTransaction);
        List<Transaction> firstExpenseTransactions = List.of(analyticsData.thirdTransaction,
            analyticsData.fourthTransaction);
        List<Transaction> secondIncomeTransactions = List.of(analyticsData.fifthTransaction,
            analyticsData.sixthTransaction);
        List<Transaction> secondExpenseTransactions = List.of(analyticsData.seventhTransaction,
            analyticsData.eighthTransaction);
        String incomeDiff = "Доходы увеличились на 8361,00 руб. (11,00%)";
        String expenseDiff = "Расходы увеличились на 30000,00 руб. (112,00%)";
        CategoriesDiffResponse salaryIncomeResponse = new CategoriesDiffResponse(ETransactionType.INCOME,
            "SALARY", "Доходы по категории SALARY не изменились.");
        CategoriesDiffResponse rentIncomeResponse = new CategoriesDiffResponse(ETransactionType.INCOME, "RENT",
            "Доходы по категории RENT уменьшились на 25083,00 руб. (100,00%)");
        CategoriesDiffResponse giftsIncomeResponse = new CategoriesDiffResponse(ETransactionType.INCOME, "GIFTS",
            "Доходы по категории GIFTS увеличились на 33444,00 руб. (100,00%)");
        List<CategoriesDiffResponse> incomeCategoriesDiffResponses = List.of(salaryIncomeResponse, rentIncomeResponse,
            giftsIncomeResponse);
        CategoriesDiffResponse foodExpenseResponse = new CategoriesDiffResponse(ETransactionType.EXPENSE, "FOOD",
            "Расходы по категории FOOD уменьшились на 10000,00 руб. (100,00%)");
        CategoriesDiffResponse taxesExpenseResponse = new CategoriesDiffResponse(ETransactionType.EXPENSE,
            "TAXES", "Расходы по категории TAXES не изменились.");
        CategoriesDiffResponse healthExpenseResponse = new CategoriesDiffResponse(ETransactionType.EXPENSE,
            "HEALTH", "Расходы по категории HEALTH увеличились на 40000,00 руб. (100,00%)");
        List<CategoriesDiffResponse> expenseCategoriesDiffResponses = List.of(foodExpenseResponse, taxesExpenseResponse,
            healthExpenseResponse);
        AnalyticsMetricsResponse expectedResponse = new AnalyticsMetricsResponse(incomeDiff, expenseDiff,
            incomeCategoriesDiffResponses, expenseCategoriesDiffResponses);
        AnalyticsMetricsRequest request = new AnalyticsMetricsRequest(analyticsData.startDateInStr,
            analyticsData.endDateInStr, analyticsData.secondStartDateInStr, analyticsData.secondEndDateInStr);

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        when(transactionService.getAmountByTransactionType(user, startDateFirst, endDateFirst, ETransactionType.INCOME))
            .thenReturn(BigDecimal.valueOf(75083));
        when(transactionService.getAmountByTransactionType(user, startDateFirst, endDateFirst, ETransactionType.EXPENSE))
            .thenReturn(BigDecimal.valueOf(26722));
        when(transactionService.getAmountByTransactionType(user, startDateSecond, endDateSecond, ETransactionType.INCOME))
            .thenReturn(BigDecimal.valueOf(83444));
        when(transactionService.getAmountByTransactionType(user, startDateSecond, endDateSecond, ETransactionType.EXPENSE))
            .thenReturn(BigDecimal.valueOf(56722));
        when(transactionService.getAllTransactionsByType(user, startDateFirst, endDateFirst, ETransactionType.INCOME))
            .thenReturn(firstIncomeTransactions);
        when(transactionService.getAllTransactionsByType(user, startDateFirst, endDateFirst, ETransactionType.EXPENSE))
            .thenReturn(firstExpenseTransactions);
        when(transactionService.getAllTransactionsByType(user, startDateSecond, endDateSecond, ETransactionType.INCOME))
            .thenReturn(secondIncomeTransactions);
        when(transactionService.getAllTransactionsByType(user, startDateSecond, endDateSecond, ETransactionType.EXPENSE))
            .thenReturn(secondExpenseTransactions);
        AnalyticsMetricsResponse actualResponse = analyticsService.getAnalyticsByMetrics(currentUser, request);

        assertEquals(expectedResponse.incomeDiff(), actualResponse.incomeDiff());
        assertEquals(expectedResponse.expenseDiff(), actualResponse.expenseDiff());
        assertTrue(expectedResponse.incomeCategoriesDiffResponses()
            .containsAll(actualResponse.incomeCategoriesDiffResponses()));
        assertTrue(expectedResponse.expenseCategoriesDiffResponses()
            .containsAll(actualResponse.expenseCategoriesDiffResponses()));
    }

    @Test
    @DisplayName("Запрос аналитики для сравнения двух периодов с некорректными датами")
    public void getAnalyticsByMetricsWithIncorrectDates() {
        TestAnalyticsData analyticsData = new TestAnalyticsData();
        User user = analyticsData.user;
        UserDetailsImpl currentUser = analyticsData.currentUser;
        AnalyticsMetricsRequest request = new AnalyticsMetricsRequest(analyticsData.endDateInStr,
            analyticsData.startDateInStr, analyticsData.secondStartDateInStr, analyticsData.secondEndDateInStr);
        String expectedExceptionMessage = "Дата начала периода не должна быть позже даты окончания";

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
            analyticsService.getAnalyticsByMetrics(currentUser, request));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }

    @Test
    @DisplayName("Запрос аналитики для сравнения двух периодов с неверным форматом дат")
    public void fetAnalyticsByMetricsWithIncorrectDateFormat() {
        TestAnalyticsData analyticsData = new TestAnalyticsData();
        User user = analyticsData.user;
        UserDetailsImpl currentUser = analyticsData.currentUser;
        AnalyticsMetricsRequest request = new AnalyticsMetricsRequest(analyticsData.startDateInStr,
            analyticsData.incorrectDateInStr, analyticsData.secondStartDateInStr, analyticsData.secondEndDateInStr);
        String expectedExceptionMessage = "Неверный формат даты (ожидается yyyy-MM-dd'T'HH:mm:ss): "
            + analyticsData.incorrectDateInStr;

        when(userService.findUserByUsername(currentUser.getUsername())).thenReturn(user);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
            analyticsService.getAnalyticsByMetrics(currentUser, request));

        assertEquals(expectedExceptionMessage, thrown.getMessage());
    }
}