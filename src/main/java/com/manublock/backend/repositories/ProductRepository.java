package com.manublock.backend.repositories;

import com.manublock.backend.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByManufacturer_Id(Long manufacturerId);
    List<Product> findByActiveTrue();
    List<Product> findByActiveTrueAndManufacturer_Id(Long manufacturerId);
}