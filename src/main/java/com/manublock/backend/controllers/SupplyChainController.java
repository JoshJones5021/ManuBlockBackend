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
        if (supplyChain.getName() == null || supplyChain.getDescription() == null) {
            throw new IllegalArgumentException("Name and description are required.");
        }
        return ResponseEntity.ok(supplyChainService.createSupplyChain(supplyChain));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplyChain> getSupplyChainById(@PathVariable Long id) {
        SupplyChain supplyChain = supplyChainService.getSupplyChain(id);
        return ResponseEntity.ok(supplyChain);
    }

    @GetMapping
    public ResponseEntity<List<SupplyChain>> getAllSupplyChains() {
        try {
            List<SupplyChain> chains = supplyChainService.getAllSupplyChains();
            System.out.println("Fetched supply chains: " + chains); // Debugging log
            return ResponseEntity.ok(chains);
        } catch (Exception e) {
            e.printStackTrace(); // Print error in logs
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplyChain> updateSupplyChain(@PathVariable Long id, @RequestBody SupplyChain updatedSupplyChain) {
        try {
            System.out.println("Received update request for Supply Chain ID: " + id);
            System.out.println("Updated Supply Chain Data: " + updatedSupplyChain);

            SupplyChain updated = supplyChainService.updateSupplyChain(id, updatedSupplyChain);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            e.printStackTrace(); // Print full error log
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
