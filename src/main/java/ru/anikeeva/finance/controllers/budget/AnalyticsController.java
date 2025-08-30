package ru.anikeeva.finance.controllers.budget;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.anikeeva.finance.dto.budget.AnalyticsCategoriesResponse;
import ru.anikeeva.finance.dto.budget.AnalyticsTransactionsResponse;
import ru.anikeeva.finance.entities.enums.ETransactionType;
import ru.anikeeva.finance.security.impl.UserDetailsImpl;
import ru.anikeeva.finance.services.budget.AnalyticsService;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Контроллер аналитики", description = "Управляет аналитикой доходов и расходов пользователя")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/transactions")
    @Operation(summary = "Получение суммарных транзакций за период",
        description = "Принимает даты начала и конца периода в формате yyyy-MM-ddTHH:mm:ss, " +
            "предоставляет суммарные значения доходов и расходов за выбранный период")
    public ResponseEntity<AnalyticsTransactionsResponse> getAnalyticsTransactions(
        @AuthenticationPrincipal UserDetailsImpl currentUser,
        @RequestParam String startDate,
        @RequestParam String endDate
    ) {
        return ResponseEntity.ok(analyticsService.getAnalyticsTransactions(currentUser, startDate, endDate));
    }

    @GetMapping("/categories")
    @Operation(summary = "Получение процентного распределения транзакций по категориям за период",
        description = "Принимает даты начала и конца периода в формате yyyy-MM-ddTHH:mm:ss и тип транзакций - доход или " +
            "расход, возвращает процентное распределение по категориям")
    public ResponseEntity<AnalyticsCategoriesResponse> getAnalyticsByCategories(
        @AuthenticationPrincipal UserDetailsImpl currentUser,
        @RequestParam String startDate,
        @RequestParam String endDate,
        @RequestParam String transactionType
    ) {
        return ResponseEntity.ok(analyticsService.getAnalyticsByCategories(currentUser, startDate, endDate,
            transactionType));
    }
}