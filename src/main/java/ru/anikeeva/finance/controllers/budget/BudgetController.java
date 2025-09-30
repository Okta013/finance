package ru.anikeeva.finance.controllers.budget;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.anikeeva.finance.dto.budget.CreateBudgetRequest;
import ru.anikeeva.finance.dto.budget.CreateBudgetResponse;
import ru.anikeeva.finance.dto.budget.ReadBudgetResponse;
import ru.anikeeva.finance.dto.budget.UpdateBudgetRequest;
import ru.anikeeva.finance.security.impl.UserDetailsImpl;
import ru.anikeeva.finance.services.budget.BudgetService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
@Tag(name = "Управление бюджетами")
@Validated
public class BudgetController {
    private final BudgetService budgetService;

    @PostMapping
    @Operation(summary = "Создание бюджета")
    public ResponseEntity<CreateBudgetResponse> createBudget(
        @AuthenticationPrincipal UserDetailsImpl currentUser,
        @RequestBody @Valid CreateBudgetRequest request
    ) {
        return ResponseEntity.ok(budgetService.createBudget(currentUser, request));
    }

    @GetMapping
    @Operation(summary = "Просмотр всех своих бюджетов")
    public ResponseEntity<Page<ReadBudgetResponse>> getBudgets(
        @AuthenticationPrincipal UserDetailsImpl currentUser,
        @RequestParam(required = false,defaultValue = "0") @Min(0) int page,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(budgetService.getAllBudgets(currentUser, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Просмотр информации о выбранном бюджете")
    public ResponseEntity<ReadBudgetResponse> getBudget(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                                        @PathVariable UUID id) {
        return ResponseEntity.ok(budgetService.getBudget(currentUser, id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Изменение полей бюджета")
    public ResponseEntity<ReadBudgetResponse> updateBudget(
        @AuthenticationPrincipal UserDetailsImpl currentUser,
        @PathVariable UUID id,
        @RequestBody @Valid UpdateBudgetRequest request
    ) {
        return ResponseEntity.ok(budgetService.updateBudget(currentUser, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удаление бюджета")
    public ResponseEntity<Void> deleteBudget(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                             @PathVariable UUID id) {
        budgetService.deleteBudget(currentUser, id);
        return ResponseEntity.noContent().build();
    }
}