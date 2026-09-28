package com.example.booking;

import com.example.booking.entity.Role;
import com.example.booking.entity.User;
import com.example.booking.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Every test here goes through the real filter chain with a genuine JWT
 * obtained from POST /auth/login — no @WithMockUser shortcuts — so the
 * security wiring (filter, entry point, access-denied handler) is what's
 * actually being exercised, not bypassed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    protected static final String ADMIN_USERNAME = "admin";
    protected static final String ADMIN_PASSWORD = "Admin@123";
    protected static final String USER_USERNAME = "user";
    protected static final String USER_PASSWORD = "User@123";

    /** A second USER account, distinct from the seeded "user", for cross-ownership tests. */
    protected static final String OTHER_USER_USERNAME = "user2";
    protected static final String OTHER_USER_PASSWORD = "User2@123";

    @BeforeEach
    void ensureSecondUserExists() {
        // DataSeeder only creates one USER; ownership tests need two independent accounts.
        if (userRepository.findByUsername(OTHER_USER_USERNAME).isEmpty()) {
            userRepository.save(User.builder()
                    .username(OTHER_USER_USERNAME)
                    .password(passwordEncoder.encode(OTHER_USER_PASSWORD))
                    .email("user2@example.com")
                    .role(Role.USER)
                    .build());
        }
    }

    protected String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginPayload(username, password));
        String response = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/login")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    private record LoginPayload(String username, String password) {}
}
