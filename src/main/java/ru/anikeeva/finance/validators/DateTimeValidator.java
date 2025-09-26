package ru.anikeeva.finance.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.anikeeva.finance.annotations.DateTimeValid;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

public class DateTimeValidator implements ConstraintValidator<DateTimeValid, String> {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            LocalDateTime.parse(value, formatter);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}