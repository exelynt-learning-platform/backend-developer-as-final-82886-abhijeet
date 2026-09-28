package com.example.booking.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidTimeRangeValidator.class)
@Documented
public @interface ValidTimeRange {
    String message() default "endTime must be after startTime";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
