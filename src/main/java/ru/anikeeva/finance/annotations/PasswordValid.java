package ru.anikeeva.finance.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import ru.anikeeva.finance.validators.PasswordValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
@Documented
public @interface PasswordValid {
    String message() default "Пароль должен состоять минимум из 8 символов: строчных и заглавных латинских букв, " +
        "цифр и спец.символов: ! @ # $ % ^ & * ( ) — _ + = ; : , ./ ? \\ | ` ~ [ ] { }";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
