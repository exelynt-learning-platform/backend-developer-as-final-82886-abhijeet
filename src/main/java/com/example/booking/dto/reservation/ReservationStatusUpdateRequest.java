package com.example.booking.dto.reservation;

import com.example.booking.entity.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public record ReservationStatusUpdateRequest(
        @NotNull(message = "status is required")
        ReservationStatus status
) {}
