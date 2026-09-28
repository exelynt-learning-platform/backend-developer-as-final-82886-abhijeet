package com.example.booking.controller;

import com.example.booking.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ResourceControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void user_canListResources_readOnly() throws Exception {
        String token = loginAndGetToken(USER_USERNAME, USER_PASSWORD);

        mockMvc.perform(get("/api/resources").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void user_cannotUpdateOrDeleteResources() throws Exception {
        String userToken = loginAndGetToken(USER_USERNAME, USER_PASSWORD);

        mockMvc.perform(put("/api/resources/1")
                        .header("Authorization", bearer(userToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Hacked Name","type":"ROOM"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/resources/1").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_hasFullCrudOverResources() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);

        // Create
        String createResponse = mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(adminToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Test Room","type":"ROOM","capacity":4,"location":"HQ"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Room"))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(createResponse).get("id").asLong();

        // Read
        mockMvc.perform(get("/api/resources/" + id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Room"));

        // Update
        mockMvc.perform(put("/api/resources/" + id)
                        .header("Authorization", bearer(adminToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Renamed Room","type":"ROOM","capacity":6,"location":"HQ"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Room"))
                .andExpect(jsonPath("$.capacity").value(6));

        // Delete
        mockMvc.perform(delete("/api/resources/" + id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        // Confirm gone
        mockMvc.perform(get("/api/resources/" + id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createResource_withoutRequiredFields_returns400() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(adminToken))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.type").exists());
    }
}
