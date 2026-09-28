package com.example.booking.specification;

import com.example.booking.entity.Reservation;
import com.example.booking.entity.ReservationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Each method returns null when its filter isn't supplied, and
 * {@link Specification#where} treats a null specification as "match all" —
 * so callers can chain {@code .and(...)} freely without null-checking.
 */
public final class ReservationSpecifications {

    private ReservationSpecifications() {}

    public static Specification<Reservation> hasStatus(ReservationStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Reservation> priceGreaterThanOrEqual(BigDecimal minPrice) {
        if (minPrice == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Reservation> priceLessThanOrEqual(BigDecimal maxPrice) {
        if (maxPrice == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Reservation> belongsToUser(Long userId) {
        if (userId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }
}
