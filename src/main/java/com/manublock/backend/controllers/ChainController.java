package com.manublock.backend.controllers;

import com.manublock.backend.dto.ChainResponse;
import com.manublock.backend.models.Chains;
import com.manublock.backend.services.ChainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/supply-chains")
public class ChainController {

    private final ChainService chainService;

    @Autowired
    public ChainController(ChainService chainService) {
        this.chainService = chainService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createSupplyChain(@RequestBody Map<String, Object> payload) {
        try {
            String name = (String) payload.get("name");
            String description = (String) payload.get("description");
            Long createdById = ((Number) payload.get("createdBy")).longValue();

            if (name == null || description == null || createdById == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Name, description, and createdBy are required.");
            }

            Chains newChain = chainService.createSupplyChain(name, description, createdById);
            return ResponseEntity.ok(newChain);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating supply chain");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChainResponse> getSupplyChainById(@PathVariable Long id) {
        return ResponseEntity.ok(chainService.getSupplyChain(id));
    }

    @GetMapping
    public List<ChainResponse> getAllSupplyChains() {
        return chainService.getAllSupplyChains();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSupplyChain(@PathVariable Long id, @RequestBody Chains updatedChains) {
        try {
            return ResponseEntity.ok(chainService.updateSupplyChain(id, updatedChains));
        } catch (Exception e) {
            e.printStackTrace(); // Print full stack trace in logs
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating supply chain: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSupplyChain(@PathVariable Long id) {
        try {
            chainService.deleteSupplyChain(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting supply chain");
        }
    }
}
