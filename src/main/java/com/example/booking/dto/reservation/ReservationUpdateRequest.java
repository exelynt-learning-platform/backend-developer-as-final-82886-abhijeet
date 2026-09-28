package com.example.booking.dto.reservation;

import com.example.booking.validation.HasTimeRange;
import com.example.booking.validation.ValidTimeRange;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Full update of a reservation's schedule/price/notes — ADMIN only. */
@ValidTimeRange
public record ReservationUpdateRequest(
        @NotNull(message = "resourceId is required")
        Long resourceId,

        @NotNull(message = "startTime is required")
        LocalDateTime startTime,

        @NotNull(message = "endTime is required")
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
