package ru.anikeeva.finance.services.budget;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.anikeeva.finance.dto.analytics.AnalyticsCategoriesResponse;
import ru.anikeeva.finance.dto.analytics.AnalyticsCategoryResponse;
import ru.anikeeva.finance.dto.analytics.AnalyticsMetricsRequest;
import ru.anikeeva.finance.dto.analytics.AnalyticsMetricsResponse;
import ru.anikeeva.finance.dto.analytics.AnalyticsTransactionsResponse;
import ru.anikeeva.finance.dto.analytics.CategoriesDiffResponse;
import ru.anikeeva.finance.entities.budget.Transaction;
import ru.anikeeva.finance.entities.enums.ETransactionType;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.security.impl.UserDetailsImpl;
import ru.anikeeva.finance.services.user.UserService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    private final TransactionService transactionService;
    private final UserService userService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public AnalyticsTransactionsResponse getAnalyticsTransactions(final UserDetailsImpl currentUser,
                                                                  final String startDateString,
                                                                  final String endDateString) {
        User user = userService.findUserByUsername(currentUser.getUsername());
        LocalDateTime startDate = parseDateTime(startDateString);
        LocalDateTime endDate = parseDateTime(endDateString);
        return new AnalyticsTransactionsResponse(
            transactionService.getAmountByTransactionType(user, startDate, endDate,  ETransactionType.INCOME),
            transactionService.getAmountByTransactionType(user, startDate, endDate, ETransactionType.EXPENSE)
        );
    }

    public AnalyticsCategoriesResponse getAnalyticsByCategories(final UserDetailsImpl currentUser,
                                                              final String startDateString,
                                                              final String endDateString,
                                                              final String transactionType) {
        User user = userService.findUserByUsername(currentUser.getUsername());
        LocalDateTime startDate = parseDateTime(startDateString);
        LocalDateTime endDate = parseDateTime(endDateString);
        List<Transaction> transactions;
        if (transactionType.equalsIgnoreCase("income")) {
            transactions = transactionService.getAllTransactionsByType(user, startDate, endDate, ETransactionType.INCOME);
        }
        else {
            transactions = transactionService.getAllTransactionsByType(user, startDate, endDate, ETransactionType.EXPENSE);
        }
        BigDecimal amount = transactions.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<AnalyticsCategoryResponse> categoriesResponses = getAnalyticsCategoryResponses(transactions, amount);
        return new AnalyticsCategoriesResponse(categoriesResponses);
    }

    public AnalyticsMetricsResponse getAnalyticsByMetrics(final UserDetailsImpl currentUser,
                                                          final AnalyticsMetricsRequest request) {
        User user = userService.findUserByUsername(currentUser.getUsername());
        LocalDateTime startDateFirst = parseDateTime(request.startDateForFirstPeriod());
        LocalDateTime endDateFirst = parseDateTime(request.endDateForFirstPeriod());
        LocalDateTime startDateSecond = parseDateTime(request.startDateForSecondPeriod());
        LocalDateTime endDateSecond = parseDateTime(request.endDateForSecondPeriod());
        BigDecimal incomeAmountByFirstPeriod = transactionService.getAmountByTransactionType(user, startDateFirst,
            endDateFirst, ETransactionType.INCOME);
        BigDecimal expenseAmountByFirstPeriod = transactionService.getAmountByTransactionType(user, startDateFirst,
            endDateFirst, ETransactionType.EXPENSE);
        BigDecimal incomeAmountBySecondPeriod = transactionService.getAmountByTransactionType(user, startDateSecond,
            endDateSecond, ETransactionType.INCOME);
        BigDecimal expenseAmountBySecondPeriod = transactionService.getAmountByTransactionType(user, startDateSecond,
            endDateSecond, ETransactionType.EXPENSE);

        String incomeDiffs =
            getMetricsDiffs(incomeAmountByFirstPeriod, incomeAmountBySecondPeriod, "Доходы");
        String expenseDiffs =
            getMetricsDiffs(expenseAmountByFirstPeriod, expenseAmountBySecondPeriod, "Расходы");

        List<Transaction> incomeTransactionsFirstPeriod = transactionService.getAllTransactionsByType(user,
            startDateFirst, endDateFirst, ETransactionType.INCOME);
        List<Transaction> expenseTransactionsFirstPeriod = transactionService.getAllTransactionsByType(user,
            startDateFirst, endDateFirst, ETransactionType.EXPENSE);
        List<Transaction> incomeTransactionsSecondPeriod = transactionService.getAllTransactionsByType(user,
            startDateSecond, endDateSecond, ETransactionType.INCOME);
        List<Transaction> expenseTransactionSecondPeriod = transactionService.getAllTransactionsByType(user,
            startDateSecond, endDateSecond, ETransactionType.EXPENSE);
        Set<String> incomeCategoriesFirstPeriod = incomeTransactionsFirstPeriod.stream()
            .map(t -> t.getCategory().name()).collect(Collectors.toSet());
        Set<String> expenseCategoriesFirstPeriod = expenseTransactionsFirstPeriod.stream()
            .map(t -> t.getCategory().name()).collect(Collectors.toSet());
        Set<String> incomeCategoriesSecondPeriod = incomeTransactionsSecondPeriod.stream()
            .map(t -> t.getCategory().name()).collect(Collectors.toSet());
        Set<String> expenseCategoriesSecondPeriod = expenseTransactionSecondPeriod.stream()
            .map(t -> t.getCategory().name()).collect(Collectors.toSet());

        List<CategoriesDiffResponse> incomeCategoriesDiffResponses = getCategoriesDiffResponses(
            incomeCategoriesFirstPeriod,
            incomeCategoriesSecondPeriod,
            incomeTransactionsFirstPeriod,
            incomeTransactionsSecondPeriod,
            "Доходы"
        );

        List<CategoriesDiffResponse> expenseCategoriesDiffResponses = getCategoriesDiffResponses(
            expenseCategoriesFirstPeriod,
            expenseCategoriesSecondPeriod,
            expenseTransactionsFirstPeriod,
            expenseTransactionSecondPeriod,
            "Расходы"
        );
        return new AnalyticsMetricsResponse(incomeDiffs, expenseDiffs, incomeCategoriesDiffResponses,
            expenseCategoriesDiffResponses);
    }

    private static List<AnalyticsCategoryResponse> getAnalyticsCategoryResponses(final List<Transaction> transactions,
                                                                                 final BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return Collections.emptyList();
        }
        Map<String, BigDecimal> sumsByCategory = transactions.stream()
            .collect(Collectors.groupingBy((Transaction t) -> t.getCategory().name(),
                    Collectors.mapping(Transaction::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        List<AnalyticsCategoryResponse> categoriesResponses = new ArrayList<>();
        for (var entry : sumsByCategory.entrySet()) {
            Double percents = entry.getValue()
                .divide(amount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
            categoriesResponses.add(new AnalyticsCategoryResponse(entry.getKey(), percents));
        }
        return categoriesResponses;
    }

    private LocalDateTime parseDateTime(final String dateTimeString) {
        try {
            return LocalDateTime.parse(dateTimeString, FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Неверный формат даты (ожидается yyyy-MM-dd'T'HH:mm:ss): " +
                dateTimeString);
        }
    }

    private BigDecimal getAmountByCategory(final String category, List<Transaction> transactions) {
        BigDecimal amount = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            if (transaction.getCategory().name().equals(category)) {
                amount = amount.add(transaction.getAmount());
            }
        }
        return amount;
    }

    private record DiffResult(
        BigDecimal diff,
        BigDecimal diffInPercents,
        Boolean isIncrease) {}

    private String getMetricsDiffs(final BigDecimal firstAmount, final BigDecimal secondAmount,
                                     final String transactionType) {
        DiffResult diffResult = getDiffResult(firstAmount, secondAmount);
        return formatDifferenceString(transactionType, null, diffResult.diff, diffResult.isIncrease,
            diffResult.diffInPercents, false);
    }

    private List<CategoriesDiffResponse> getCategoriesDiffResponses(final Set<String> categoriesFirstPeriod,
                                                                    final Set<String> categoriesSecondPeriod,
                                                                    final List<Transaction> transactionsFirstPeriod,
                                                                    final List<Transaction> transactionsSecondPeriod,
                                                                    final String transactionType) {
        ETransactionType eTransactionType = transactionType.equalsIgnoreCase("доходы")
            ? ETransactionType.INCOME
            : ETransactionType.EXPENSE;
        Set<String> allCategoriesFromTransactions = new HashSet<>(categoriesFirstPeriod);
        allCategoriesFromTransactions.addAll(categoriesSecondPeriod);
        List<CategoriesDiffResponse> categoriesDiffResponses = new ArrayList<>();
        for (String category : allCategoriesFromTransactions) {
            BigDecimal firstAmount = getAmountByCategory(category, transactionsFirstPeriod);
            BigDecimal secondAmount = getAmountByCategory(category, transactionsSecondPeriod);
            DiffResult diffResult = getDiffResult(firstAmount, secondAmount);
            String categoryDiff = formatDifferenceString(transactionType, category, diffResult.diff,
                diffResult.isIncrease, diffResult.diffInPercents,
                true);
            categoriesDiffResponses.add(new CategoriesDiffResponse(eTransactionType, category, categoryDiff));
        }
        return categoriesDiffResponses;
    }

    private String formatDifferenceString(final String transactionType, final String category,
                                          final BigDecimal diffAmount, final Boolean isIncrease, BigDecimal diffInPercents,
                                          boolean isCategoryLevel) {
        if (isIncrease == null) {
            if (isCategoryLevel) {
                return String.format("%s по категории %s не изменились.", transactionType, category);
            }
            else {
                return String.format("%s не изменились.", transactionType);
            }
        }

        String direction = isIncrease ? "увеличились" : "уменьшились";

        if (isCategoryLevel) {
            return String.format("%s по категории %s %s на %.2f руб. (%.2f%%)", transactionType, category, direction,
                diffAmount.doubleValue(), diffInPercents.doubleValue());
        }
        else {
            return String.format("%s %s на %.2f руб. (%.2f%%)", transactionType, direction, diffAmount.doubleValue(),
                diffInPercents.doubleValue());
        }
    }

    private DiffResult getDiffResult(final BigDecimal firstAmount, final BigDecimal secondAmount) {
        BigDecimal diff;
        BigDecimal diffInPercents;
        boolean isIncrease;

        int comparison = firstAmount.compareTo(secondAmount);
        if (comparison == 0) {
            return new DiffResult(BigDecimal.ZERO, BigDecimal.ZERO, null);
        }
        if (firstAmount.compareTo(BigDecimal.ZERO) == 0) {
            diff = secondAmount.abs();
            diffInPercents = BigDecimal.valueOf(100);
            isIncrease = true;
        }
        else {
            diff = secondAmount.subtract(firstAmount).abs();
            diffInPercents = diff
                .divide(firstAmount, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            isIncrease = secondAmount.compareTo(firstAmount) > 0;
        }
        return new DiffResult(diff, diffInPercents, isIncrease);
    }
}