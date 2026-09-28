package com.example.booking.repository;

import com.example.booking.entity.Reservation;
import com.example.booking.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long>,
        JpaSpecificationExecutor<Reservation> {

    /**
     * Any existing, non-cancelled reservation on the given resource whose window
     * overlaps [startTime, endTime). Used to reject double-bookings with 409.
     * excludeId lets an update ignore the reservation being edited.
     */
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.resource.id = :resourceId
              AND r.status <> :cancelledStatus
              AND (:excludeId IS NULL OR r.id <> :excludeId)
              AND r.startTime < :endTime
              AND r.endTime > :startTime
            """)
    List<Reservation> findOverlapping(@Param("resourceId") Long resourceId,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime,
                                       @Param("excludeId") Long excludeId,
                                       @Param("cancelledStatus") ReservationStatus cancelledStatus);
}
