package com.mottu.ops.domain.model;

import com.mottu.ops.domain.exception.DomainValidationException;

import java.util.UUID;

public class Yard {
    private final UUID id;
    private final String name;
    private final String city;
    private final String address;
    private boolean active;

    public Yard(UUID id, String name, String city, String address, boolean active) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("Yard name cannot be blank");
        }
        if (city == null || city.isBlank()) {
            throw new DomainValidationException("Yard city cannot be blank");
        }
        if (address == null || address.isBlank()) {
            throw new DomainValidationException("Yard address cannot be blank");
        }

        this.id = id != null ? id : UUID.randomUUID();
        this.name = name.trim();
        this.city = city.trim();
        this.address = address.trim();
        this.active = active;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public boolean isActive() {
        return active;
    }
}
