package com.shoppingmall.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ShopOwnerFieldsValidator.class)
public @interface ShopOwnerFields {

    String message() default "Check the shop-owner fields.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
