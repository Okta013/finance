package ru.anikeeva.finance.services.budget;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.anikeeva.finance.dto.budget.CreateTransactionRequest;
import ru.anikeeva.finance.dto.budget.CreateTransactionResponse;
import ru.anikeeva.finance.dto.budget.TransactionResponse;
import ru.anikeeva.finance.dto.budget.UpdateTransactionRequest;
import ru.anikeeva.finance.entities.budget.CurrencyRate;
import ru.anikeeva.finance.entities.budget.Transaction;
import ru.anikeeva.finance.entities.enums.ETransactionType;
import ru.anikeeva.finance.entities.user.User;
import ru.anikeeva.finance.exceptions.EmptyRequestException;
import ru.anikeeva.finance.exceptions.EntityNotFoundException;
import ru.anikeeva.finance.exceptions.InsufficientFundsException;
import ru.anikeeva.finance.exceptions.IntegrationException;
import ru.anikeeva.finance.exceptions.NoRightsException;
import ru.anikeeva.finance.mappers.TransactionMapper;
import ru.anikeeva.finance.repositories.budget.TransactionRepository;
import ru.anikeeva.finance.repositories.user.UserRepository;
import ru.anikeeva.finance.security.impl.UserDetailsImpl;
import ru.anikeeva.finance.services.user.UserService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Currency;
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
    private final JobLauncher asyncJobLauncher;
    private final Job importJob;
    private final CurrencyRateService currencyRateService;
    private final BudgetService budgetService;

    @Transactional
    public CreateTransactionResponse createTransaction(final UserDetailsImpl currentUser,
                                                       final CreateTransactionRequest request) {
        User user = userService.findUserByUsername(currentUser.getUsername());
        checkBalanceForTransaction(currentUser, request.type(), request.initialAmount());
        BigDecimal amountInBaseCurrency = request.initialCurrency().equals(user.getBaseCurrency())
            ? request.initialAmount()
            : calculateAmountWithBaseCurrency(user, request.initialAmount(), request.initialCurrency());
        budgetService.checkBudgetNotExceeded(user, request.category(), amountInBaseCurrency);
        Transaction transaction = transactionMapper.toTransaction(request);
        transaction.setAmountInBaseCurrency(amountInBaseCurrency);
        transaction.setUser(user);
        transactionRepository.save(transaction);
        log.info("Создана {}-транзакция {} на сумму {} {} для пользователя {}", request.type(), transaction.getId(),
            request.initialAmount(), request.initialCurrency(), user.getUsername());
        switch (request.type()) {
            case INCOME -> user.setBalance(user.getBalance().add(request.initialAmount()));
            case EXPENSE -> user.setBalance(user.getBalance().subtract(request.initialAmount()));
        }
        userRepository.save(user);
        log.info("Баланс пользователя изменен после выполнения транзакции {}", transaction.getId());
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
        if (request.type() == null && request.category() == null && request.initialAmount() == null &&
            request.initialCurrency() == null && request.dateTime() == null && request.description() == null) {
            log.info("Попытка изменить транзакцию {} с пустым запросом", transaction.getId());
            throw new EmptyRequestException("Запрос на изменение транзакции пустой");
        }
        ETransactionType type = request.type() != null ? request.type() : transaction.getType();
        checkBalanceForTransaction(currentUser, type, request.initialAmount());
        transactionMapper.updateTransactionFromUpdateTransactionRequest(request, transaction);
        transactionRepository.save(transaction);
        log.info("Детали транзакции {} были изменены пользователем", transaction.getId());
        return transactionMapper.toTransactionResponse(transaction);
    }

    public void deleteTransaction(final UserDetailsImpl currentUser, final UUID transactionId) {
        Transaction transaction = findTransactionForUser(currentUser, transactionId);
        transactionRepository.delete(transaction);
        log.info("Транзакция {} была удалена пользователем", transaction.getId());
    }

    public BigDecimal getAmountByTransactionType(final User user, final LocalDateTime startDate,
                                                 final LocalDateTime endDate, final ETransactionType transactionType) {
        return transactionRepository.findAllByUserIdAndTypeAndDateTimeBetween(user.getId(), transactionType, startDate,
            endDate).stream()
            .map(Transaction::getAmountInBaseCurrency).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Transaction> getAllTransactionsByType(final User user, final LocalDateTime startDate,
                                                      final LocalDateTime endDate, ETransactionType type) {
        return transactionRepository.findAllByUserIdAndTypeAndDateTimeBetween(user.getId(), type, startDate, endDate);
    }

    public String uploadFileWithTransactions(final UserDetailsImpl currentUser, final MultipartFile file) {
        try {
            Path tempFile = Files.createTempFile("transactions-", ".csv");
            file.transferTo(tempFile);
            JobParameters params = new JobParametersBuilder()
                .addString("input.file.path", tempFile.toString())
                .addString("userId", currentUser.getId().toString())
                .addLong("time", System.currentTimeMillis())
                .addString("jobId", UUID.randomUUID().toString())
                .toJobParameters();
            asyncJobLauncher.run(importJob, params);
            log.info("Инициирована загрузка csv-файла с транзакциями пользователем {}", currentUser.getUsername());
            return "Импорт файла запущен";
        } catch (Exception e) {
            log.error("Ошибка загрузки csv-файла пользователем {}", currentUser.getUsername(), e);
            throw new IntegrationException("Ошибка загрузки файла .csv");
        }
    }

    public BigDecimal calculateAmountWithBaseCurrency(final User user, final BigDecimal amount, final Currency currency) {
        Currency baseCurrency = user.getBaseCurrency();
        CurrencyRate currencyRate = currencyRateService.getCurrencyRateByCurrency(currency);
        BigDecimal rateInRub = currencyRate.getValueInRelationToBaseCurrency();
        if (baseCurrency.equals(Currency.getInstance("RUB"))) return amount.multiply(rateInRub);
        else {
            CurrencyRate baseCurrencyRate = currencyRateService.getCurrencyRateByCurrency(baseCurrency);
            return amount.multiply(rateInRub).divide(baseCurrencyRate.getValueInRelationToBaseCurrency(), 2,
                RoundingMode.HALF_UP);
        }
    }

    private Transaction findTransactionForUser(final UserDetailsImpl currentUser, final UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(() ->
            new EntityNotFoundException("Транзакция не найдена"));
        if (!transaction.getUser().getId().equals(currentUser.getId())) {
            log.info("Попытка получить транзакцию другого пользователя со стороны {}", currentUser.getUsername());
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