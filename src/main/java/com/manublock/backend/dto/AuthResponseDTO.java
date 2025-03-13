package com.manublock.backend.dto;

public class AuthResponseDTO {
    private String token;
    private String walletAddress;

    public AuthResponseDTO(String token, String walletAddress) {
        this.token = token;
        this.walletAddress = walletAddress;
    }

    public String getToken() {
        return token;
    }

    public String getWalletAddress() {
        return walletAddress;
    }
}