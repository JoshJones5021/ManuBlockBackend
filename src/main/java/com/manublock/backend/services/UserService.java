package com.manublock.backend.services;

import com.manublock.backend.dto.RegisterUserRequest;
import com.manublock.backend.dto.UserResponse;
import com.manublock.backend.models.Users;
import com.manublock.backend.repositories.UserRepository;
import com.manublock.backend.utils.CustomException;
import com.manublock.backend.utils.PasswordUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.manublock.backend.models.Roles;  // Import new Role enum

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Users authenticateUser(String email, String rawPassword) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!PasswordUtil.matches(rawPassword, user.getPassword())) {
            System.out.println("Password mismatch detected");
            throw new RuntimeException("Invalid credentials");
        }

        return user;
    }

    public void registerUser(RegisterUserRequest request) {
        // Check if a user with the same email exists
        Optional<Users> existingUserByEmail = userRepository.findByEmail(request.getEmail());
        if (existingUserByEmail.isPresent()) {
            throw new CustomException("User with this email already exists.");
        }

        // Check if a user with the same username exists
        Optional<Users> existingUserByUsername = userRepository.findByUsername(request.getUsername());
        if (existingUserByUsername.isPresent()) {
            throw new CustomException("User with this username already exists.");
        }

        Users user = new Users();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Set a default role if not provided
        if (request.getRole() == null) {
            user.setRole(Roles.CUSTOMER);
        } else {
            user.setRole(request.getRole());
        }

        userRepository.save(user);
    }

    public Optional<Users> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Users connectWallet(Long userId, String walletAddress) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setWalletAddress(walletAddress);
        return userRepository.save(user);
    }

    public Users assignRole(Long userId, Roles role) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(role);  // Assign role correctly
        return userRepository.save(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::new) // ✅ Calls constructor with Users object
                .collect(Collectors.toList());
    }

    public Users updateUser(Long userId, Map<String, String> updates) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updates.containsKey("email")) {
            user.setEmail(updates.get("email"));
        }

        return userRepository.save(user);
    }
}