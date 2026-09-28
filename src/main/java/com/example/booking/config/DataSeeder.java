package com.example.booking.config;

import com.example.booking.entity.*;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds one ADMIN and one USER account (see README for credentials), plus a
 * couple of sample resources so the API is immediately exercisable after
 * startup. Only runs against an empty users table, so it's safe on every
 * restart and never overwrites data you've since changed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("Admin@123"))
                    .email("admin@example.com")
                    .role(Role.ADMIN)
                    .build());

            userRepository.save(User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("User@123"))
                    .email("user@example.com")
                    .role(Role.USER)
                    .build());

            log.info("Seeded default users: admin/Admin@123 (ROLE_ADMIN), user/User@123 (ROLE_USER)");
        }

        if (resourceRepository.count() == 0) {
            resourceRepository.save(Resource.builder()
                    .name("Conference Room A")
                    .type(ResourceType.ROOM)
                    .description("8-seat conference room with a projector")
                    .location("Building 1, Floor 2")
                    .capacity(8)
                    .active(true)
                    .build());

            resourceRepository.save(Resource.builder()
                    .name("Company Van 1")
                    .type(ResourceType.VEHICLE)
                    .description("6-seat van for site visits")
                    .location("Parking Garage B")
                    .capacity(6)
                    .active(true)
                    .build());

            resourceRepository.save(Resource.builder()
                    .name("Portable Projector")
                    .type(ResourceType.EQUIPMENT)
                    .description("HDMI/USB-C projector, carrying case included")
                    .location("Equipment Room")
                    .capacity(null)
                    .active(true)
                    .build());

            log.info("Seeded 3 sample resources");
        }
    }
}
