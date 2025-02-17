// src/main/java/com/manublock/backend/repositories/EdgeRepository.java

package com.manublock.backend.repositories;

import com.manublock.backend.models.Edge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EdgeRepository extends JpaRepository<Edge, Long> {
}