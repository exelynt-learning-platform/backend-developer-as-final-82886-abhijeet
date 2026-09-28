package com.example.booking.dto.resource;

import com.example.booking.entity.Resource;
import com.example.booking.entity.ResourceType;

import java.time.Instant;

public record ResourceResponse(
        Long id,
        String name,
        ResourceType type,
        String description,
        String location,
        Integer capacity,
        boolean active,
        Instant createdAt
) {
    public static ResourceResponse from(Resource r) {
        return new ResourceResponse(
                r.getId(), r.getName(), r.getType(), r.getDescription(),
                r.getLocation(), r.getCapacity(), r.isActive(), r.getCreatedAt());
    }
}
