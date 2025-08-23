package ru.anikeeva.finance.services.budget;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.anikeeva.finance.dto.budget.AnalyticsTransactionsResponse;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.security.impl.UserDetailsImpl;
import ru.anikeeva.finance.services.user.UserService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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
        return new AnalyticsTransactionsResponse(transactionService.getAllIncome(user, startDate, endDate),
            transactionService.getAllExpense(user, startDate, endDate));
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