package ru.anikeeva.finance.mappers;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.lang.NonNull;
import ru.anikeeva.finance.dto.budget.TransactionImportDto;
import ru.anikeeva.finance.exceptions.BadDataException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TransactionFieldSetMapper implements FieldSetMapper<TransactionImportDto> {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    @NonNull
    public TransactionImportDto mapFieldSet(FieldSet fieldSet) {
        String dateStr = fieldSet.readString("date_time");
        LocalDateTime dateTime = null;
        if (!dateStr.isEmpty()) {
            try {
                dateTime = LocalDateTime.parse(dateStr.trim(), formatter);
            } catch (Exception e) {
                throw new BadDataException("Неверный формат даты/времени: " + dateStr);
            }
        }
        return new TransactionImportDto(
            fieldSet.readString("type"),
            fieldSet.readString("category"),
            fieldSet.readBigDecimal("amount"),
            fieldSet.readString("currency"),
            dateTime,
            fieldSet.readString("description")
        );
    }
}