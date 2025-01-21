package com.manublock.backend.models;

import jakarta.persistence.*;

@Entity
@Table(name = "users")  // Change table name to "users" instead of "user"
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String email;
    private String password;
    private String walletAddress;

    @Enumerated(EnumType.STRING)
    private Role role;

    public enum Role {
        ADMIN, SUPPLIER, MANUFACTURER, DISTRIBUTOR, CUSTOMER
    }

    // Default Constructor
    public User() {
    }

    // Parameterized Constructor
    public User(String username, String email, String password, String walletAddress, Role role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.walletAddress = walletAddress;
        this.role = role;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    // Override toString for debugging purposes
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", walletAddress='" + walletAddress + '\'' +
                ", role=" + role +
                '}';
    }
}
