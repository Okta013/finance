package ru.anikeeva.finance.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.anikeeva.finance.annotations.PasswordMatches;
import ru.anikeeva.finance.dto.registration.SignUpRequest;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {
    @Override
    public void initialize(PasswordMatches constraintAnnotation) {
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        SignUpRequest req = (SignUpRequest) obj;
        return req.password().equals(req.confirmPassword());
    }
}