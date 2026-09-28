package com.example.booking.dto.resource;

import com.example.booking.entity.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ResourceRequest(
        @NotBlank(message = "name is required")
        @Size(max = 150)
        String name,

        @NotNull(message = "type is required")
        ResourceType type,

        @Size(max = 1000)
        String description,

        @Size(max = 200)
        String location,

        @Positive(message = "capacity must be a positive number")
        Integer capacity,

        Boolean active
) {}
