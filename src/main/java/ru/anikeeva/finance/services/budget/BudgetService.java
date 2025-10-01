package ru.anikeeva.finance.services.budget;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.anikeeva.finance.dto.budget.CreateBudgetRequest;
import ru.anikeeva.finance.dto.budget.CreateBudgetResponse;
import ru.anikeeva.finance.dto.budget.ReadBudgetResponse;
import ru.anikeeva.finance.dto.budget.UpdateBudgetRequest;
import ru.anikeeva.finance.dto.notifications.BudgetNotification;
import ru.anikeeva.finance.entities.budget.Budget;
import ru.anikeeva.finance.entities.budget.Transaction;
import ru.anikeeva.finance.entities.enums.EBudgetPeriod;
import ru.anikeeva.finance.entities.enums.ETransactionCategory;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.exceptions.BadDataException;
import ru.anikeeva.finance.exceptions.BudgetLimitExceedingException;
import ru.anikeeva.finance.exceptions.EntityNotFoundException;
import ru.anikeeva.finance.exceptions.IntegrationException;
import ru.anikeeva.finance.exceptions.NoRightsException;
import ru.anikeeva.finance.mappers.BudgetMapper;
import ru.anikeeva.finance.repositories.budget.BudgetRepository;
import ru.anikeeva.finance.repositories.budget.TransactionRepository;
import ru.anikeeva.finance.security.impl.UserDetailsImpl;
import ru.anikeeva.finance.services.user.UserService;
import ru.anikeeva.finance.services.websocket.WebSocketNotificationService;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final UserService userService;
    private final BudgetMapper budgetMapper;
    private final TransactionRepository transactionRepository;
    private final WebSocketNotificationService notificationService;

    private final static BigDecimal EXCESS_PERCENTAGE = BigDecimal.valueOf(0.8);

    public CreateBudgetResponse createBudget(final UserDetailsImpl currentUser, final CreateBudgetRequest request) {
        User user = userService.findUserByUsername(currentUser.getUsername());
        if (budgetRepository.existsByUserAndPeriodAndCategory(user, request.period(), request.category())) {
            throw new BadDataException("Бюджет по этой категории на выбранный период уже существует");
        }
        Budget budget = budgetMapper.fromCreateBudgetRequest(request);
        budget.setUser(user);
        budgetRepository.save(budget);
        return new CreateBudgetResponse(budget.getId(), true);
    }

    public Page<ReadBudgetResponse> getAllBudgets(final UserDetailsImpl currentUser, final int page, final int size) {
        User user = userService.findUserByUsername(currentUser.getUsername());
        Pageable pageable = PageRequest.of(page, size);
        Page<Budget> budgets = budgetRepository.findAllByUser(user, pageable);
        return budgets.map(budgetMapper::fromBudget);
    }

    public ReadBudgetResponse getBudget(UserDetailsImpl currentUser, final UUID id) {
        User user = userService.findUserByUsername(currentUser.getUsername());
        Budget budget = findBudgetById(id);
        if (!user.getId().equals(budget.getUser().getId())) {
            log.info("Попытка запросить бюджет другого пользователя со стороны {}", currentUser.getUsername());
            throw new NoRightsException("У пользователя нет прав на просмотр выбранного бюджета");
        }
        return budgetMapper.fromBudget(budget);
    }

    public ReadBudgetResponse updateBudget(final UserDetailsImpl currentUser, final UUID id,
                                           final UpdateBudgetRequest request) {
        if (request.limitAmount() == null && request.category() == null && request.period() == null) {
            throw new IllegalArgumentException("Запрос на изменение бюджета пуст");
        }
        Budget budget = findBudgetById(id);
        if (!currentUser.getId().equals(budget.getUser().getId())) {
            log.info("Попытка изменения бюджета другого пользователя со стороны {}", currentUser.getUsername());
            throw new NoRightsException("У пользователя нет прав на изменение выбранного бюджета");
        }
        budgetMapper.updateBudgetFromUpdateBudgetRequest(request, budget);
        budgetRepository.save(budget);
        return budgetMapper.fromBudget(budget);
    }

    public void deleteBudget(final UserDetailsImpl currentUser, final UUID id) {
        User user = userService.findUserByUsername(currentUser.getUsername());
        Budget budget = findBudgetById(id);
        if (!user.getId().equals(budget.getUser().getId())) {
            log.info("Попытка удаления бюджета другого пользователя со стороны {}", currentUser.getUsername());
            throw new NoRightsException("У пользователя нет прав на удаление выбранного бюджета");
        }
        budgetRepository.delete(budget);
    }

    public void checkBudgetNotExceeded(final User user, final ETransactionCategory category, final BigDecimal amount) {
        List<Budget> userBudgets = budgetRepository.findAllByUserAndCategory(user, category);
        List<EBudgetPeriod> periods = userBudgets.stream().map(Budget::getPeriod).toList();
        for (EBudgetPeriod period : periods) {
            checkBudgetByPeriod(user, category, amount, period, userBudgets);
        }
    }

    private Budget findBudgetById(final UUID id) {
        return budgetRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Бюджет не найден"));
    }

    private void checkBudgetByPeriod(final User user, final ETransactionCategory category, final BigDecimal amount,
                                     final EBudgetPeriod period, List<Budget> budgets) {
        LocalDate now = LocalDate.now();
        LocalDateTime startDate;
        LocalDateTime endDate;
        String periodStr;
        switch (period) {
            case DAY: {
                startDate = LocalDateTime.of(now, LocalTime.MIN);
                endDate = LocalDateTime.of(now, LocalTime.MAX);
                periodStr = "дневной";
                break;
            }
            case WEEK: {
                startDate = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
                endDate = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(LocalTime.MAX);
                periodStr = "недельный";
                break;
            }
            case MONTH: {
                startDate = now.withDayOfMonth(1).atStartOfDay();
                endDate = now.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
                periodStr = "месячный";
                break;
            }
            case YEAR: {
                startDate = now.withDayOfYear(1).atStartOfDay();
                endDate = now.with(TemporalAdjusters.lastDayOfYear()).atTime(LocalTime.MAX);
                periodStr = "годовой";
                break;
            }
            default: {
                startDate = null;
                endDate = null;
                periodStr = null;
            }
        }
        if (startDate == null || endDate == null || periodStr == null) {
            throw new IntegrationException("Не удалось рассчитать бюджет");
        }
        Budget periodBudget = budgets.stream().filter(budget -> budget.getPeriod().equals(period))
            .findFirst().orElseThrow(() -> new EntityNotFoundException("Бюджет на период не найден"));

        BigDecimal periodAmount = transactionRepository.findAllByUserAndCategoryAndDateTimeBetween(user, category,
            startDate, endDate).stream().map(Transaction::getInitialAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal consumed = periodAmount.add(amount);
        BigDecimal limit = periodBudget.getLimitAmount();
        BigDecimal threshold = limit.multiply(EXCESS_PERCENTAGE);

        if (consumed.compareTo(threshold) >= 0 && consumed.compareTo(limit) < 0) {
            notificationService.sendBudgetWarning(user.getId().toString(), new BudgetNotification(
                String.format("Внимание! Вы израсходовали более %.0f%% бюджета по категории %s", 80.0, category.name()),
                limit.subtract(periodAmount),
                category.name()
            ));
        }

        if (consumed.compareTo(limit) > 0) {
            notificationService.sendBudgetWarning(user.getId().toString(), new BudgetNotification(
                String.format("Лимит бюджета по категории %s превышен! Новые транзакции добавить невозможно",
                    category.name()),
                BigDecimal.ZERO,
                category.name()
            ));
            throw new BudgetLimitExceedingException(String.format("Превышен %s лимит расходов по категории, " +
                "доступный остаток %.2f руб.", periodStr, limit.subtract(periodAmount)));
        }
    }
}