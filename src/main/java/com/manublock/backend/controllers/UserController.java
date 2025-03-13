package com.manublock.backend.controllers;

import com.manublock.backend.dto.UserResponseDTO;
import com.manublock.backend.models.Roles;
import com.manublock.backend.models.Users;
import com.manublock.backend.services.UserService;
import com.manublock.backend.utils.JwtUtil;
import com.manublock.backend.dto.AuthResponseDTO;
import com.manublock.backend.dto.LoginRequestDTO;
import com.manublock.backend.dto.RegisterUserRequestDTO;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.manublock.backend.repositories.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public UserController(UserService userService, JwtUtil jwtUtil, UserRepository userRepository) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterUserRequestDTO request) {
        userService.registerUser(request);
        return ResponseEntity.ok().body("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> loginUser(@RequestBody LoginRequestDTO loginRequestDTO) {
        Users user = userService.authenticateUser(loginRequestDTO.getEmail(), loginRequestDTO.getPassword());
        String token = jwtUtil.generateToken(user);

        // Include walletAddress in the response
        return ResponseEntity.ok(new AuthResponseDTO(token, user.getWalletAddress()));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String token) {
        // Add token to blacklist (implement a proper token blacklist)
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Logout successful. Token invalidated.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<Optional<Users>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Users> updateUser(@PathVariable Long id, @RequestBody Map<String, String> updates) {
        Users updatedUser = userService.updateUser(id, updates);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/{id}/connect-wallet")
    public ResponseEntity<Users> connectWallet(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String walletAddress = request.get("walletAddress");
        Users updatedUser = userService.connectWallet(id, walletAddress.isEmpty() ? null : walletAddress);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/{id}/assign-role")
    public ResponseEntity<Users> assignRole(@PathVariable Long id, @RequestParam String role) {
        Roles userRole = Roles.valueOf(role.toUpperCase());
        Users updatedUser = userService.assignRole(id, userRole);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<String> dashboard() {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .body("Dashboard content");
    }

    @GetMapping(value = {"/", ""})
    public ResponseEntity<?> getAllUsers(@RequestParam(required = false) String role) {
        try {
            List<Users> users;

            if (role != null && !role.isEmpty()) {
                // Get users by role if role parameter is provided
                try {
                    Roles userRole = Roles.valueOf(role.toUpperCase());
                    users = userRepository.findByRole(userRole);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + role));
                }
            } else {
                // Get all users if no role is specified
                users = userRepository.findAll();
            }

            // Convert to DTOs to prevent circular references
            List<UserResponseDTO> userDTOs = users.stream()
                    .map(UserResponseDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(userDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving users: " + e.getMessage()));
        }
    }

    @GetMapping("/roles")
    public ResponseEntity<Roles[]> getAllRoles() {
        return ResponseEntity.ok(Roles.values());
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id);  // ✅ Directly return the ResponseEntity from the service
    }
}