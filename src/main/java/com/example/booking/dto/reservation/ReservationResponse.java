package com.example.booking.dto.reservation;

import com.example.booking.entity.Reservation;
import com.example.booking.entity.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long resourceId,
        String resourceName,
        Long userId,
        String username,
        LocalDateTime startTime,
        LocalDateTime endTime,
        ReservationStatus status,
        BigDecimal price,
        String notes,
        Instant createdAt
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getResource().getId(),
                r.getResource().getName(),
                r.getUser().getId(),
                r.getUser().getUsername(),
                r.getStartTime(),
                r.getEndTime(),
                r.getStatus(),
                r.getPrice(),
                r.getNotes(),
                r.getCreatedAt()
        );
    }
}
