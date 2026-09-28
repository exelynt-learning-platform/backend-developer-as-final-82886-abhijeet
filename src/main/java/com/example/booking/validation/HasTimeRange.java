package com.example.booking.validation;

import java.time.LocalDateTime;

/** Implemented by any request DTO that carries a start/end window, so {@link ValidTimeRange} can check it generically. */
public interface HasTimeRange {
    LocalDateTime getStartTime();
    LocalDateTime getEndTime();
}
