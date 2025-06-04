package ru.anikeeva.finance.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.anikeeva.finance.annotations.PasswordValid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<PasswordValid, Object> {
    private Pattern pattern;
    private Matcher matcher;
    private static final String EMAIL_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).{8,}$";
    @Override
    public void initialize(PasswordValid constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
        String password = (String) o;
        pattern = Pattern.compile(EMAIL_PATTERN);
        matcher = pattern.matcher(password);
        return matcher.matches();
    }
}