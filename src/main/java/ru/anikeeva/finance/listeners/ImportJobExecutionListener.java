package ru.anikeeva.finance.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import ru.anikeeva.finance.entities.budget.Transaction;
import ru.anikeeva.finance.entities.enums.ETransactionType;
import ru.anikeeva.finance.repositories.budget.TransactionRepository;
import ru.anikeeva.finance.services.user.UserService;
import ru.anikeeva.finance.services.websocket.WebSocketNotificationService;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class ImportJobExecutionListener implements JobExecutionListener {
    private final UserService userService;
    private final TransactionRepository transactionRepository;
    private final WebSocketNotificationService notificationService;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Старт Batch Job: {}", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        BatchStatus status = jobExecution.getStatus();
        String userIdStr = jobExecution.getJobParameters().getString("userId");
        if (status == BatchStatus.COMPLETED) {
            log.info("Batch Job успешно завершена: {}", jobName);
            Long jobId = jobExecution.getJobId();
            if (userIdStr != null) {
                UUID userId = UUID.fromString(userIdStr);
                List<Transaction> transactions = transactionRepository.findAllByJobId(jobId);
                BigDecimal totalChange = transactions.stream()
                    .map(tx -> tx.getType() == ETransactionType.INCOME
                        ? tx.getInitialAmount()
                        : tx.getInitialAmount().negate())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                userService.recalculateBalance(userId, totalChange);
                notificationService.notifyJobCompletion(UUID.fromString(userIdStr),
                    "Импорт файла успешно завершен!"
                );
            }
        } else if (status == BatchStatus.FAILED) {
            log.error("Batch Job провалена: {}", jobName);
            jobExecution.getAllFailureExceptions().forEach(ex -> log.error("Причина ошибки: ", ex));
            if (userIdStr != null) {
                notificationService.notifyJobCompletion(
                    UUID.fromString(userIdStr),
                    "Импорт файла завершился с ошибкой."
                );
            }
        }
        try {
            String filePathStr = jobExecution.getJobParameters().getString("input.file.path");
            if (filePathStr != null) {
                Path filePath = Paths.get(filePathStr);
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("Файл {} удалён после завершения Job", filePath);
                }
            }
        } catch (IOException e) {
            log.error("Ошибка удаления временного файла после выполнения Job: {}", jobName, e);
        }
    }
}