package com.manublock.backend.dto;

import com.manublock.backend.models.Users;

public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;
    private String walletAddress;
    private String role;

    public UserResponseDTO(Users user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.walletAddress = user.getWalletAddress();
        this.role = user.getRole().name();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getWalletAddress() { return walletAddress; }
    public String getRole() { return role; }
}
