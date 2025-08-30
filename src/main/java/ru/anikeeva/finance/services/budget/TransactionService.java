package ru.anikeeva.finance.services.budget;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.anikeeva.finance.dto.budget.CreateTransactionRequest;
import ru.anikeeva.finance.dto.budget.CreateTransactionResponse;
import ru.anikeeva.finance.dto.budget.TransactionResponse;
import ru.anikeeva.finance.dto.budget.UpdateTransactionRequest;
import ru.anikeeva.finance.entities.budget.Transaction;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    private final UserService userService;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final UserRepository userRepository;

    @Transactional
    public CreateTransactionResponse createTransaction(final UserDetailsImpl currentUser,
                                                       final CreateTransactionRequest request) {
        User user = userService.findUserByUsername(currentUser.getUsername());
        checkBalanceForTransaction(currentUser, request.type(), request.amount());
        Transaction transaction = Transaction.builder()
            .user(user)
            .type(request.type())
            .category(request.category())
            .amount(request.amount())
            .dateTime(request.dateTime())
            .description(request.description())
            .build();
        if (request.currency() != null) {
            transaction.setCurrency(request.currency());
        }
        transactionRepository.save(transaction);
        switch (request.type()) {
            case INCOME -> user.setBalance(user.getBalance().add(request.amount()));
            case EXPENSE -> user.setBalance(user.getBalance().subtract(request.amount()));
        }
        userRepository.save(user);
        return new CreateTransactionResponse(transaction.getId(), true);
    }

    public TransactionResponse showTransaction(final UserDetailsImpl currentUser, final UUID transactionId) {
        Transaction transaction = findTransactionForUser(currentUser, transactionId);
        return transactionMapper.toTransactionResponse(transaction);
    }

    public Page<TransactionResponse> showAllTransactions(final UserDetailsImpl currentUser,
                                                         final int page,
                                                         final int limit) {
        Page<Transaction> transactions =
            transactionRepository.findAllByUserId(currentUser.getId(), PageRequest.of(page, limit));
        return transactions.map(transactionMapper::toTransactionResponse);
    }

    public TransactionResponse updateTransaction(final UserDetailsImpl currentUser, final UUID transactionId,
                                                 final UpdateTransactionRequest request) {
        Transaction transaction = findTransactionForUser(currentUser, transactionId);
        if (request.type() == null && request.category() == null && request.amount() == null &&
            request.currency() == null && request.dateTime() == null && request.description() == null) {
            throw new EmptyRequestException("Запрос на изменение транзакции пустой");
        }
        ETransactionType type = request.type() != null ? request.type() : transaction.getType();
        checkBalanceForTransaction(currentUser, type, request.amount());
        transactionMapper.updateTransactionFromUpdateTransactionRequest(request, transaction);
        transactionRepository.save(transaction);
        return transactionMapper.toTransactionResponse(transaction);
    }

    public void deleteTransaction(final UserDetailsImpl currentUser, final UUID transactionId) {
        Transaction transaction = findTransactionForUser(currentUser, transactionId);
        transactionRepository.delete(transaction);
    }

    public BigDecimal getAmountByTransactionType(final User user, final LocalDateTime startDate,
                                                 final LocalDateTime endDate, final ETransactionType transactionType) {
        return transactionRepository.findAllByUserIdAndTypeAndDateTimeBetween(user.getId(), transactionType, startDate,
            endDate).stream()
            .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Transaction> getAllTransactionsByType(final User user, final LocalDateTime startDate,
                                                      final LocalDateTime endDate, ETransactionType type) {
        return transactionRepository.findAllByUserIdAndTypeAndDateTimeBetween(user.getId(), type, startDate, endDate);
    }

    private Transaction findTransactionForUser(final UserDetailsImpl currentUser, final UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(() ->
            new EntityNotFoundException("Транзакция не найдена"));
        if (!transaction.getUser().getId().equals(currentUser.getId())) {
            throw new NoRightsException("Транзакция не принадлежит текущему пользователю");
        }
        return transaction;
    }

    private void checkBalanceForTransaction(final UserDetailsImpl currentUser, final ETransactionType type,
                                            final BigDecimal amount) {
        User user = userService.findUserByUsername(currentUser.getUsername());
        if (type.equals(ETransactionType.EXPENSE) && user.getBalance().compareTo(amount) < 0) {
            log.info("Откат транзакции пользователя {} из-за недостаточного баланса. Сумма транзакции: {}, баланс: {}.",
                user.getUsername(), amount, user.getBalance());
            throw new InsufficientFundsException("Баланс пользователя меньше суммы транзакции");
        }
    }
}