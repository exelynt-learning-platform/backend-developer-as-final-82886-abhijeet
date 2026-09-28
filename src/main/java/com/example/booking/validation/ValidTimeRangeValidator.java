package com.example.booking.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidTimeRangeValidator implements ConstraintValidator<ValidTimeRange, HasTimeRange> {

    @Override
    public boolean isValid(HasTimeRange value, ConstraintValidatorContext context) {
        if (value == null || value.getStartTime() == null || value.getEndTime() == null) {
            // @NotNull on the individual fields reports those; nothing more to say here.
            return true;
        }
        boolean valid = value.getEndTime().isAfter(value.getStartTime());
        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("endTime must be after startTime")
                    .addPropertyNode("endTime")
                    .addConstraintViolation();
        }
        return valid;
    }
}
