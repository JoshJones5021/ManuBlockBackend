package com.manublock.backend.dto;

import com.manublock.backend.models.Users;

class ManufacturerDTO {
    private Long id;
    private String username;

    public ManufacturerDTO(Users manufacturer) {
        this.id = manufacturer.getId();
        this.username = manufacturer.getUsername();
    }

    // Getters
    public Long getId() { return id; }
    public String getUsername() { return username; }
}