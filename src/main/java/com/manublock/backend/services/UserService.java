package com.manublock.backend.services;

import com.manublock.backend.dto.RegisterUserRequestDTO;
import com.manublock.backend.dto.UserResponseDTO;
import com.manublock.backend.models.Chains;
import com.manublock.backend.models.Nodes;
import com.manublock.backend.models.Roles;
import com.manublock.backend.models.Users;
import com.manublock.backend.repositories.NodeRepository;
import com.manublock.backend.repositories.UserRepository;
import com.manublock.backend.utils.CustomException;
import com.manublock.backend.utils.PasswordUtil;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final NodeRepository nodeRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, NodeRepository nodeRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.nodeRepository = nodeRepository;
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

    public void registerUser(RegisterUserRequestDTO request) {
        Optional<Users> existingUserByEmail = userRepository.findByEmail(request.getEmail());
        if (existingUserByEmail.isPresent()) {
            throw new CustomException("User with this email already exists.");
        }

        Optional<Users> existingUserByUsername = userRepository.findByUsername(request.getUsername());
        if (existingUserByUsername.isPresent()) {
            throw new CustomException("User with this username already exists.");
        }

        Users user = new Users();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() == null ? Roles.CUSTOMER : request.getRole());

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
        user.setRole(role);
        return userRepository.save(user);
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDTO::new)
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

    @Transactional
    public ResponseEntity<?> deleteUser(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == Roles.ADMIN) {
            long adminCount = userRepository.countByRole(Roles.ADMIN);
            if (adminCount <= 1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        Map.of("error", "Cannot delete the last remaining admin.")
                );
            }
        }

        List<Nodes> assignedNodes = nodeRepository.findByAssignedUser_Id(userId);
        if (!assignedNodes.isEmpty()) {
            Nodes firstNode = assignedNodes.get(0);
            Chains supplyChain = firstNode.getSupplyChain();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of("error", "❌ Cannot delete user since they are assigned to supply chain: " +
                            supplyChain.getName() + ", ID: " + supplyChain.getId())
            );
        }

        userRepository.delete(user);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }
}
