package com.example.booking.service;

import com.example.booking.dto.reservation.ReservationCreateRequest;
import com.example.booking.dto.reservation.ReservationResponse;
import com.example.booking.dto.reservation.ReservationStatusUpdateRequest;
import com.example.booking.dto.reservation.ReservationUpdateRequest;
import com.example.booking.entity.Reservation;
import com.example.booking.entity.ReservationStatus;
import com.example.booking.entity.Resource;
import com.example.booking.entity.User;
import com.example.booking.exception.ConflictException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.security.AuthenticatedUserProvider;
import com.example.booking.security.SecurityUser;
import com.example.booking.specification.ReservationSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ResourceService resourceService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public ReservationResponse create(ReservationCreateRequest request) {
        SecurityUser currentUser = authenticatedUserProvider.getCurrentUser();
        Resource resource = resourceService.findEntity(request.resourceId());

        assertNoOverlap(resource.getId(), request.startTime(), request.endTime(), null);

        User userRef = userRepository.getReferenceById(currentUser.getId());

        Reservation reservation = Reservation.builder()
                .resource(resource)
                .user(userRef)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(ReservationStatus.PENDING)
                .price(request.price())
                .notes(request.notes())
                .build();

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    public Page<ReservationResponse> list(ReservationStatus status, BigDecimal minPrice, BigDecimal maxPrice,
                                           Pageable pageable) {
        SecurityUser currentUser = authenticatedUserProvider.getCurrentUser();

        Specification<Reservation> spec = Specification.where(ReservationSpecifications.hasStatus(status))
                .and(ReservationSpecifications.priceGreaterThanOrEqual(minPrice))
                .and(ReservationSpecifications.priceLessThanOrEqual(maxPrice));

        // Ownership is enforced in the query itself, not by filtering the page
        // after the fact, so pagination totals stay correct for a USER.
        if (!authenticatedUserProvider.isAdmin()) {
            spec = spec.and(ReservationSpecifications.belongsToUser(currentUser.getId()));
        }

        return reservationRepository.findAll(spec, pageable).map(ReservationResponse::from);
    }

    public ReservationResponse get(Long id) {
        Reservation reservation = findEntity(id);
        assertReadable(reservation);
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse update(Long id, ReservationUpdateRequest request) {
        // ADMIN-only route (enforced by @PreAuthorize on the controller method).
        Reservation reservation = findEntity(id);
        Resource resource = resourceService.findEntity(request.resourceId());

        assertNoOverlap(resource.getId(), request.startTime(), request.endTime(), reservation.getId());

        reservation.setResource(resource);
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setPrice(request.price());
        reservation.setNotes(request.notes());

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse updateStatus(Long id, ReservationStatusUpdateRequest request) {
        // ADMIN-only route (enforced by @PreAuthorize on the controller method).
        Reservation reservation = findEntity(id);
        reservation.setStatus(request.status());
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse cancel(Long id) {
        Reservation reservation = findEntity(id);
        assertReadable(reservation); // same rule: owner or admin

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ConflictException("Reservation " + id + " is already cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Transactional
    public void delete(Long id) {
        // ADMIN-only route (enforced by @PreAuthorize on the controller method).
        if (!reservationRepository.existsById(id)) {
            throw new ResourceNotFoundException("No reservation found with id " + id);
        }
        reservationRepository.deleteById(id);
    }

    private void assertNoOverlap(Long resourceId, java.time.LocalDateTime start, java.time.LocalDateTime end, Long excludeId) {
        List<Reservation> overlapping = reservationRepository.findOverlapping(
                resourceId, start, end, excludeId, ReservationStatus.CANCELLED);
        if (!overlapping.isEmpty()) {
            throw new ConflictException(
                    "Resource " + resourceId + " already has a reservation overlapping that time window");
        }
    }

    /** Owner or ADMIN; anyone else gets 403 rather than learning the reservation exists. */
    private void assertReadable(Reservation reservation) {
        if (authenticatedUserProvider.isAdmin()) return;
        SecurityUser currentUser = authenticatedUserProvider.getCurrentUser();
        if (!reservation.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have access to this reservation");
        }
    }

    private Reservation findEntity(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No reservation found with id " + id));
    }
}
