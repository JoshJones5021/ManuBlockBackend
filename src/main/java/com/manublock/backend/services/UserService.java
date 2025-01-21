package com.manublock.backend.services;

import com.manublock.backend.dto.RegisterUserRequest;
import com.manublock.backend.models.User;
import com.manublock.backend.repositories.UserRepository;
import com.manublock.backend.utils.CustomException;
import com.manublock.backend.utils.PasswordUtil;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User authenticateUser(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!PasswordUtil.matches(rawPassword, user.getPassword())) {
            System.out.println("Password mismatch detected");
            throw new RuntimeException("Invalid credentials");
        }

        return user;
    }


    @Transactional
    public User registerUser(RegisterUserRequest request) {
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());

        if (existingUser.isPresent()) {
            throw new CustomException("User with this email already exists.");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(PasswordUtil.hashPassword(request.getPassword()));

        return userRepository.save(user);
    }


    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User connectWallet(Long userId, String walletAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setWalletAddress(walletAddress);
        return userRepository.save(user);
    }

    public User assignRole(Long userId, User.Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(role);
        return userRepository.save(user);
    }
}
