package com.example.booking.controller;

import com.example.booking.BaseIntegrationTest;
import com.example.booking.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReservationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ResourceRepository resourceRepository;

    private Long resourceId;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @BeforeEach
    void pickAResource() {
        resourceId = resourceRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Expected DataSeeder to have created at least one resource"))
                .getId();
    }

    private String reservationJson(long resourceId, LocalDateTime start, LocalDateTime end, String price) {
        return """
                {"resourceId": %d, "startTime": "%s", "endTime": "%s", "price": %s}
                """.formatted(resourceId, start.format(FMT), end.format(FMT), price);
    }

    // ---------- ownership: identity comes from the JWT, never the request body ----------

    @Test
    void createReservation_ownerIsTakenFromToken_notFromRequestBody() throws Exception {
        String userToken = loginAndGetToken(USER_USERNAME, USER_PASSWORD);
        Long realUserId = userRepository.findByUsername(USER_USERNAME).orElseThrow().getId();

        LocalDateTime start = LocalDateTime.now().plusDays(10);
        LocalDateTime end = start.plusHours(2);

        String response = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(APPLICATION_JSON)
                        .content(reservationJson(resourceId, start, end, "100.00")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long actualOwnerId = objectMapper.readTree(response).get("userId").asLong();
        org.assertj.core.api.Assertions.assertThat(actualOwnerId).isEqualTo(realUserId);
    }

    @Test
    void createReservation_withInjectedUserIdField_isRejectedNotSilentlyAccepted() throws Exception {
        String userToken = loginAndGetToken(USER_USERNAME, USER_PASSWORD);
        LocalDateTime start = LocalDateTime.now().plusDays(11);
        LocalDateTime end = start.plusHours(1);

        // The DTO has no userId field at all, so Jackson rejects the unrecognized
        // property outright — an attacker cannot smuggle in someone else's id.
        String maliciousBody = """
                {"resourceId": %d, "startTime": "%s", "endTime": "%s", "price": 50.00, "userId": 999999}
                """.formatted(resourceId, start.format(FMT), end.format(FMT));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(APPLICATION_JSON)
                        .content(maliciousBody))
                .andExpect(status().isBadRequest());
    }

    // ---------- validation ----------

    @Test
    void createReservation_withEndBeforeStart_returns400() throws Exception {
        String userToken = loginAndGetToken(USER_USERNAME, USER_PASSWORD);
        LocalDateTime start = LocalDateTime.now().plusDays(12);
        LocalDateTime end = start.minusHours(1);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(APPLICATION_JSON)
                        .content(reservationJson(resourceId, start, end, "50.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.endTime").exists());
    }

    @Test
    void createReservation_withNegativePrice_returns400() throws Exception {
        String userToken = loginAndGetToken(USER_USERNAME, USER_PASSWORD);
        LocalDateTime start = LocalDateTime.now().plusDays(13);
        LocalDateTime end = start.plusHours(1);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(APPLICATION_JSON)
                        .content(reservationJson(resourceId, start, end, "-5.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    // ---------- double-booking ----------

    @Test
    void overlappingReservationOnSameResource_returns409() throws Exception {
        String userToken = loginAndGetToken(USER_USERNAME, USER_PASSWORD);
        LocalDateTime start = LocalDateTime.now().plusDays(20);
        LocalDateTime end = start.plusHours(3);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(APPLICATION_JSON)
                        .content(reservationJson(resourceId, start, end, "75.00")))
                .andExpect(status().isCreated());

        // Overlaps the first booking's window by an hour.
        LocalDateTime overlapStart = start.plusHours(1);
        LocalDateTime overlapEnd = overlapStart.plusHours(3);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(APPLICATION_JSON)
                        .content(reservationJson(resourceId, overlapStart, overlapEnd, "75.00")))
                .andExpect(status().isConflict());
    }

    // ---------- ownership scoping on read ----------

    @Test
    void user_seesOnlyOwnReservations_adminSeesAll() throws Exception {
        String userToken = loginAndGetToken(USER_USERNAME, USER_PASSWORD);
        String otherToken = loginAndGetToken(OTHER_USER_USERNAME, OTHER_USER_PASSWORD);
        String adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);

        LocalDateTime start1 = LocalDateTime.now().plusDays(30);
        createReservation(userToken, start1, start1.plusHours(1), "10.00");

        LocalDateTime start2 = LocalDateTime.now().plusDays(31);
        createReservation(otherToken, start2, start2.plusHours(1), "10.00");

        // "user" only ever sees their own, no matter how many other reservations exist.
        mockMvc.perform(get("/api/reservations").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].username", everyItem(is(USER_USERNAME))));

        // ADMIN sees at least both of the ones just created.
        String adminList = mockMvc.perform(get("/api/reservations")
                        .header("Authorization", bearer(adminToken))
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long distinctUsernames = objectMapper.readTree(adminList).get("content").findValuesAsText("username")
                .stream().distinct().count();
        org.assertj.core.api.Assertions.assertThat(distinctUsernames).isGreaterThanOrEqualTo(2);
    }

    @Test
    void user_cannotViewAnotherUsersReservationById_returns403() throws Exception {
        String otherToken = loginAndGetToken(OTHER_USER_USERNAME, OTHER_USER_PASSWORD);
        LocalDateTime start = LocalDateTime.now().plusDays(40);
        long reservationId = createReservation(otherToken, start, start.plusHours(1), "20.00");

        String userToken = loginAndGetToken(USER_USERNAME, USER_PASSWORD);
        mockMvc.perform(get("/api/reservations/" + reservationId).header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());

        // But the owner and an admin can both see it.
        mockMvc.perform(get("/api/reservations/" + reservationId).header("Authorization", bearer(otherToken)))
                .andExpect(status().isOk());

        String adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        mockMvc.perform(get("/api/reservations/" + reservationId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    // ---------- status transitions ----------

    @Test
    void user_canCancelOwnReservation_butNotChangeStatusDirectly() throws Exception {
        String userToken = loginAndGetToken(USER_USERNAME, USER_PASSWORD);
        LocalDateTime start = LocalDateTime.now().plusDays(50);
        long reservationId = createReservation(userToken, start, start.plusHours(1), "30.00");

        // USER cannot set arbitrary status directly.
        mockMvc.perform(patch("/api/reservations/" + reservationId + "/status")
                        .header("Authorization", bearer(userToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"status":"CONFIRMED"}
                                """))
                .andExpect(status().isForbidden());

        // But can cancel their own.
        mockMvc.perform(patch("/api/reservations/" + reservationId + "/cancel")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Cancelling an already-cancelled reservation is a conflict, not a silent no-op.
        mockMvc.perform(patch("/api/reservations/" + reservationId + "/cancel")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isConflict());
    }

    @Test
    void admin_canConfirmAReservation() throws Exception {
        String userToken = loginAndGetToken(USER_USERNAME, USER_PASSWORD);
        LocalDateTime start = LocalDateTime.now().plusDays(51);
        long reservationId = createReservation(userToken, start, start.plusHours(1), "30.00");

        String adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        mockMvc.perform(patch("/api/reservations/" + reservationId + "/status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"status":"CONFIRMED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void user_cannotDeleteOrPutReservations() throws Exception {
        String userToken = loginAndGetToken(USER_USERNAME, USER_PASSWORD);
        LocalDateTime start = LocalDateTime.now().plusDays(52);
        long reservationId = createReservation(userToken, start, start.plusHours(1), "30.00");

        mockMvc.perform(delete("/api/reservations/" + reservationId).header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/reservations/" + reservationId)
                        .header("Authorization", bearer(userToken))
                        .contentType(APPLICATION_JSON)
                        .content(reservationJson(resourceId, start, start.plusHours(2), "30.00")))
                .andExpect(status().isForbidden());
    }

    // ---------- filtering + pagination ----------

    @Test
    void filteringByStatusAndPriceRange_andPagination_work() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        String userToken = loginAndGetToken(USER_USERNAME, USER_PASSWORD);

        for (int i = 0; i < 3; i++) {
            LocalDateTime start = LocalDateTime.now().plusDays(60 + i);
            createReservation(userToken, start, start.plusHours(1), i == 0 ? "500.00" : "10.00");
        }

        // minPrice filters out the two cheap ones.
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", bearer(adminToken))
                        .param("minPrice", "100")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].price", everyItem(comparesEqualTo(500.00))));

        // Pagination: size=1 should return exactly one item per page.
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.content.length()").value(1));

        // Invalid sort field is rejected rather than silently ignored or 500ing.
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", bearer(userToken))
                        .param("sort", "password"))
                .andExpect(status().isBadRequest());
    }

    private long createReservation(String token, LocalDateTime start, LocalDateTime end, String price) throws Exception {
        String response = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(reservationJson(resourceId, start, end, price)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }
}
