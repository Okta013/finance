package ru.anikeeva.finance.dto.budget;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Дто-класс для реализации пакетного импорта транзакций из CSV-файлов")
public record TransactionImportDto(
    @Schema(description = "Тип транзакции - доход или расход", allowableValues = {"INCOME", "EXPENSE"})
    String type,

    @Schema(description = "Категория транзакции")
    String category,

    @Schema(description = "Сумма транзакции")
    BigDecimal initialAmount,

    @Schema(description = "Валюта транзакции")
    String initialCurrency,

    @Schema(description = "Дата и время транзакции в формате YYYY-MM-DD'T'HH:mm:ss")
    LocalDateTime dateTime,

    @Schema(description = "Описание транзакции (не обязательное поле)")
    String description
)
{}