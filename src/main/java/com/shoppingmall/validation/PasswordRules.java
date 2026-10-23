package com.shoppingmall.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordRules.Validator.class)
public @interface PasswordRules {

    String message() default "Use 8 to 72 characters with at least one letter and one digit.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    final class Validator implements ConstraintValidator<PasswordRules, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || FieldRules.password(value);
        }
    }
}
