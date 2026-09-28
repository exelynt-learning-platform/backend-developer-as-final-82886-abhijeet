package com.example.booking.controller;

import com.example.booking.dto.common.PageResponse;
import com.example.booking.dto.reservation.ReservationCreateRequest;
import com.example.booking.dto.reservation.ReservationResponse;
import com.example.booking.dto.reservation.ReservationStatusUpdateRequest;
import com.example.booking.dto.reservation.ReservationUpdateRequest;
import com.example.booking.entity.ReservationStatus;
import com.example.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Set;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Bookings against resources")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    /** Prevents ?sort=someInternalOrInvalidField from either erroring oddly or leaking columns. */
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "startTime", "endTime", "price", "status", "createdAt");

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Create a reservation; the owner is always the caller from the JWT, never client input")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationCreateRequest request) {
        ReservationResponse created = reservationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "List reservations — ADMIN sees all, USER sees only their own",
            description = "Supports filtering by status/minPrice/maxPrice and pagination/sorting via page, size, sort")
    public ResponseEntity<PageResponse<ReservationResponse>> list(
            @Parameter(description = "Filter by status") @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "Minimum price (inclusive)") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price (inclusive)") @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        validateSortFields(pageable);
        return ResponseEntity.ok(PageResponse.from(reservationService.list(status, minPrice, maxPrice, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Get a reservation by id — ADMIN can view any, USER only their own (403 otherwise)")
    public ResponseEntity<ReservationResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a reservation's schedule/price/notes (ADMIN only)")
    public ResponseEntity<ReservationResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody ReservationUpdateRequest request) {
        return ResponseEntity.ok(reservationService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change a reservation's status, e.g. confirm it (ADMIN only)")
    public ResponseEntity<ReservationResponse> updateStatus(@PathVariable Long id,
                                                              @Valid @RequestBody ReservationStatusUpdateRequest request) {
        return ResponseEntity.ok(reservationService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Cancel a reservation — owner or ADMIN only")
    public ResponseEntity<ReservationResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.cancel(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Permanently delete a reservation (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void validateSortFields(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Cannot sort by '" + order.getProperty() + "'. Allowed fields: " + SORTABLE_FIELDS);
            }
        }
    }
}
