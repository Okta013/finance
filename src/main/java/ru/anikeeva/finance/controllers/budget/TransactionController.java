package ru.anikeeva.finance.controllers.budget;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.anikeeva.finance.dto.budget.CreateTransactionRequest;
import ru.anikeeva.finance.dto.budget.CreateTransactionResponse;
import ru.anikeeva.finance.dto.budget.TransactionResponse;
import ru.anikeeva.finance.dto.budget.UpdateTransactionRequest;
import ru.anikeeva.finance.security.impl.UserDetailsImpl;
import ru.anikeeva.finance.services.budget.TransactionService;

import java.util.UUID;

@Tag(name = "Транзакции", description = "Контроллер управления транзакциями")
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @Operation(summary = "Создание транзакции", description = "Создает новую транзакцию для текущего пользователя")
    @PostMapping
    public ResponseEntity<CreateTransactionResponse> createTransaction(
        @AuthenticationPrincipal UserDetailsImpl currentUser, @RequestBody CreateTransactionRequest request
    )
    {
        return ResponseEntity.ok(transactionService.createTransaction(currentUser, request));
    }

    @Operation(summary = "Просмотр транзакции", description = "Отображает информацию о выбранной транзакции")
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> findTransactionById(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                                                   @PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.showTransaction(currentUser, id));
    }

    @Operation(summary = "Просмотр всех транзакций", description = "Отображает список транзакций текущего пользователя")
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> findTransactions(
        @AuthenticationPrincipal UserDetailsImpl currentUser, @RequestParam int page, @RequestParam int limit) {
        return ResponseEntity.ok(transactionService.showAllTransactions(currentUser, page, limit));
    }

    @Operation(summary = "Изменение транзакции",
        description = "Изменяет указанные поля выбранной транзакции текущего пользователя")
    @PatchMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                                                 @PathVariable UUID id,
                                                                 @RequestBody UpdateTransactionRequest request) {
        return ResponseEntity.ok(transactionService.updateTransaction(currentUser, id, request));
    }

    @Operation(summary = "Удаление транзакции", description = "Удаляет выбранную транзакцию текущего пользователя")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                                  @PathVariable UUID id) {
        transactionService.deleteTransaction(currentUser, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Импорт транзакций из файла .csv",
        description = "Позволяет загрузить файл .csv с транзакциями, который инициирует batch-обработку и запись в базу")
    @PostMapping("/import")
    public ResponseEntity<String> uploadFileWithTransactions(@AuthenticationPrincipal UserDetailsImpl currentUser,
                                                             @RequestParam MultipartFile file) {
        return ResponseEntity.ok(transactionService.uploadFileWithTransactions(currentUser, file));
    }
}