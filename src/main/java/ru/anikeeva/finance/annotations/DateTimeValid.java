package ru.anikeeva.finance.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import ru.anikeeva.finance.validators.DateTimeValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateTimeValidator.class)
public @interface DateTimeValid {
    String message() default "Некорректный формат даты и времени. Требуемый формат: yyyy-MM-ddTHH:mm:ss";
    Class<?> [] groups() default {};
    Class<? extends Payload>[] payload() default {};
}