package ru.anikeeva.finance.configs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import ru.anikeeva.finance.dto.budget.TransactionImportDto;
import ru.anikeeva.finance.entities.budget.Transaction;
import ru.anikeeva.finance.exceptions.BadDataException;
import ru.anikeeva.finance.listeners.ImportJobExecutionListener;
import ru.anikeeva.finance.listeners.TransactionSkipListener;
import ru.anikeeva.finance.mappers.TransactionFieldSetMapper;
import ru.anikeeva.finance.repositories.budget.TransactionRepository;
import ru.anikeeva.finance.services.user.UserService;
import ru.anikeeva.finance.services.websocket.WebSocketNotificationService;

import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
@Slf4j
@RequiredArgsConstructor
public class BatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Step importStep(FlatFileItemReader<TransactionImportDto> reader,
                           ItemProcessor<TransactionImportDto, Transaction> processor,
                           JdbcBatchItemWriter<Transaction> writer,
                           TransactionSkipListener skipListener) {
        return new StepBuilder("importStep", jobRepository)
            .<TransactionImportDto, Transaction>chunk(100, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            .skip(BadDataException.class)
            .skipLimit(10)
            .listener(skipListener)
            .build();
    }

    @Bean
    public Job importJob(JobRepository jobRepository, Step importStep, JobExecutionListener jobListener) {
        return new JobBuilder("importJob", jobRepository)
            .start(importStep)
            .listener(jobListener)
            .build();
    }

    @Bean
    public JobExecutionListener importJobExecutionListener(UserService userService,
                                                           TransactionRepository transactionRepository,
                                                           WebSocketNotificationService notificationService) {
        return new ImportJobExecutionListener(userService, transactionRepository, notificationService);
    }

    @Bean
    @StepScope
    public FlatFileItemReader<TransactionImportDto> transactionItemReader(@Value("#{jobParameters['input.file.path']}")
                                                                              String path) {
        FlatFileItemReader<TransactionImportDto> reader = new FlatFileItemReader<>();
        reader.setEncoding("UTF-8");
        reader.setResource(new FileSystemResource(path));
        reader.setLinesToSkip(1);
        DefaultLineMapper<TransactionImportDto> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("type", "category", "initialAmount", "initialCurrency", "date_time", "description");
        tokenizer.setDelimiter(",");
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(new TransactionFieldSetMapper());
        reader.setLineMapper(lineMapper);
        return reader;
    }

    @Bean
    public JdbcBatchItemWriter<Transaction> transactionItemWriter(DataSource dataSource) {
        JdbcBatchItemWriter<Transaction> writer = new JdbcBatchItemWriter<>();
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        String sql = "INSERT INTO transaction (id, user_id, type, category, initialAmount, initialCurrency, date_time, description, job_id) " +
            "VALUES (:id, :user.id, :typeAsString, :categoryAsString, :initialAmount, :currencyCode, :dateTime, :description, :jobId)";
        writer.setSql(sql);
        writer.setDataSource(dataSource);
        writer.afterPropertiesSet();
        return writer;
    }

    @Bean
    public JobLauncher asyncJobLauncher() throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }
}