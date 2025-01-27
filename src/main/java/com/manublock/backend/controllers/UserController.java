package com.manublock.backend.controllers;

import com.manublock.backend.models.Role;
import com.manublock.backend.models.User;
import com.manublock.backend.services.UserService;
import com.manublock.backend.utils.JwtUtil;
import com.manublock.backend.dto.AuthResponse;
import com.manublock.backend.dto.LoginRequest;
import com.manublock.backend.dto.RegisterUserRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

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
        User user = userService.authenticateUser(loginRequest.getEmail(), loginRequest.getPassword());
        String token = jwtUtil.generateToken(user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String token) {
        // Add token to blacklist (implement a proper token blacklist)
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Logout successful. Token invalidated.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<Optional<User>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping("/{id}/connect-wallet")
    public ResponseEntity<User> connectWallet(@PathVariable Long id, @RequestBody String walletAddress) {
        User updatedUser = userService.connectWallet(id, walletAddress);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/{id}/assign-role")
    public ResponseEntity<User> assignRole(@PathVariable Long id, @RequestParam String role) {
        Role userRole = Role.valueOf(role.toUpperCase());
        User updatedUser = userService.assignRole(id, userRole);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<String> dashboard() {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .body("Dashboard content");
    }
}
