package com.manublock.backend.controllers;

import com.manublock.backend.dto.UserResponse;
import com.manublock.backend.models.Roles;
import com.manublock.backend.models.Users;
import com.manublock.backend.services.UserService;
import com.manublock.backend.utils.JwtUtil;
import com.manublock.backend.dto.AuthResponse;
import com.manublock.backend.dto.LoginRequest;
import com.manublock.backend.dto.RegisterUserRequest;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterUserRequest request) {
        userService.registerUser(request);
        return ResponseEntity.ok().body("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(@RequestBody LoginRequest loginRequest) {
        Users user = userService.authenticateUser(loginRequest.getEmail(), loginRequest.getPassword());
        String token = jwtUtil.generateToken(user);

        // Include walletAddress in the response
        return ResponseEntity.ok(new AuthResponse(token, user.getWalletAddress()));
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
    public ResponseEntity<?> getAllUsers() {
        try {
            List<UserResponse> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch users"));
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