package ru.anikeeva.finance.services.budget;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.anikeeva.finance.dto.budget.AnalyticsCategoriesResponse;
import ru.anikeeva.finance.dto.budget.AnalyticsCategoryResponse;
import ru.anikeeva.finance.dto.budget.AnalyticsTransactionsResponse;
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
        Set<String> categoriesFromTransactions = transactions.stream()
            .map(t -> t.getCategory().name())
            .collect(Collectors.toSet());
        List<AnalyticsCategoryResponse> categoriesResponses = getAnalyticsCategoryResponses(transactions, amount);
        return new AnalyticsCategoriesResponse(categoriesResponses);
    }

    private static List<AnalyticsCategoryResponse> getAnalyticsCategoryResponses(List<Transaction> transactions,
                                                                                 BigDecimal amount) {
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

    private LocalDateTime parseDateTime(String dateTimeString) {
        try {
            return LocalDateTime.parse(dateTimeString, FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Неверный формат даты (ожидается yyyy-MM-dd'T'HH:mm:ss): " +
                dateTimeString);
        }
    }

}