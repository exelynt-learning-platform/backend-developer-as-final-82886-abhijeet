package com.example.booking.dto.reservation;

import com.example.booking.validation.HasTimeRange;
import com.example.booking.validation.ValidTimeRange;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Deliberately has no userId field: ownership always comes from the JWT
 * principal in the controller/service, never from client input.
 */
@ValidTimeRange
public record ReservationCreateRequest(
        @NotNull(message = "resourceId is required")
        Long resourceId,

        @NotNull(message = "startTime is required")
        @Future(message = "startTime must be in the future")
        LocalDateTime startTime,

        @NotNull(message = "endTime is required")
        @Future(message = "endTime must be in the future")
        LocalDateTime endTime,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "price must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "price may have at most 2 decimal places")
        BigDecimal price,

        @Size(max = 500)
        String notes
) implements HasTimeRange {

    @Override
    public LocalDateTime getStartTime() {
        return startTime;
    }

    @Override
    public LocalDateTime getEndTime() {
        return endTime;
    }
}
