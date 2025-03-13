package com.manublock.backend.repositories;

import com.manublock.backend.models.Users;
import com.manublock.backend.models.Roles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByEmail(String email);
    Optional<Users> findByUsername(String username);
    Optional<Users> findByWalletAddress(String walletAddress);
    List<Users> findByRole(Roles role);

    // ✅ Count number of users by role (for admin deletion check)
    long countByRole(Roles role);
}