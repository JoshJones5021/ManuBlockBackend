package com.manublock.backend.dto;

import com.manublock.backend.models.Users;

public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String walletAddress;

    public UserResponse(Users user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.walletAddress = user.getWalletAddress();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getWalletAddress() { return walletAddress; }
}
