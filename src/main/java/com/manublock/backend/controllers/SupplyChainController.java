package com.manublock.backend.controllers;

import com.manublock.backend.models.SupplyChain;
import com.manublock.backend.services.SupplyChainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supply-chains")
public class SupplyChainController {

    private final SupplyChainService supplyChainService;

    @Autowired
    public SupplyChainController(SupplyChainService supplyChainService) {
        this.supplyChainService = supplyChainService;
    }

    @PostMapping("/create")
    public ResponseEntity<SupplyChain> createSupplyChain(@RequestBody SupplyChain supplyChain) {
        try {
            if (supplyChain.getName() == null || supplyChain.getDescription() == null || supplyChain.getCreatedBy() == null) {
                throw new IllegalArgumentException("Name, description, and createdBy are required.");
            }
            return ResponseEntity.ok(supplyChainService.createSupplyChain(supplyChain, supplyChain.getCreatedBy()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplyChain> getSupplyChainById(@PathVariable Long id) {
        return ResponseEntity.ok(supplyChainService.getSupplyChain(id));
    }

    @GetMapping
    public ResponseEntity<List<SupplyChain>> getAllSupplyChains() {
        try {
            return ResponseEntity.ok(supplyChainService.getAllSupplyChains());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplyChain> updateSupplyChain(@PathVariable Long id, @RequestBody SupplyChain updatedSupplyChain) {
        try {
            return ResponseEntity.ok(supplyChainService.updateSupplyChain(id, updatedSupplyChain));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplyChain(@PathVariable Long id) {
        try {
            supplyChainService.deleteSupplyChain(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
