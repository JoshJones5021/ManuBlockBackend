package com.manublock.backend.controllers;

import com.manublock.backend.dto.ChainResponseDTO;
import com.manublock.backend.dto.EdgeResponseDTO;
import com.manublock.backend.dto.NodeResponseDTO;
import com.manublock.backend.models.Chains;
import com.manublock.backend.services.BlockchainService;
import com.manublock.backend.services.ChainService;
import com.manublock.backend.services.SupplyChainFinalizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/supply-chains")
public class ChainController {

    private final ChainService chainService;
    private final BlockchainService blockchainService;
    private final SupplyChainFinalizationService finalizationService;

    @Autowired
    public ChainController(ChainService chainService, BlockchainService blockchainService,
                           SupplyChainFinalizationService finalizationService) {
        this.chainService = chainService;
        this.blockchainService = blockchainService;
        this.finalizationService = finalizationService;
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

            // Create in PostgreSQL - blockchain creation is handled inside the service
            Chains newChain = chainService.createSupplyChain(name, description, createdById);

            // Check if blockchain creation failed
            if ("FAILED".equals(newChain.getBlockchainStatus())) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to create supply chain on blockchain. Please try again."));
            }

            return ResponseEntity.ok(newChain);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating supply chain: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/blockchain-status")
    public ResponseEntity<?> getBlockchainStatus(@PathVariable Long id) {
        try {
            Map<String, Object> status = chainService.getBlockchainStatus(id);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving blockchain status: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChainResponseDTO> getSupplyChainById(@PathVariable Long id) {
        return ResponseEntity.ok(chainService.getSupplyChain(id));
    }

    @GetMapping
    public List<ChainResponseDTO> getAllSupplyChains() {
        return chainService.getAllSupplyChains();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSupplyChain(@PathVariable Long id) {
        try {
            // First check if supply chain is finalized
            boolean isFinalized = finalizationService.isSupplyChainFinalized(id);
            if (isFinalized) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Cannot delete a finalized supply chain");
            }

            chainService.deleteSupplyChain(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting supply chain");
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getSupplyChainsByUserId(@PathVariable Long userId) {
        try {
            List<ChainResponseDTO> supplyChains = chainService.findSupplyChainsByUserId(userId);
            return ResponseEntity.ok(supplyChains);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch supply chains for user: " + e.getMessage()));
        }
    }

    // This is a partial update focusing on the finalize endpoint
    @PostMapping("/{id}/finalize")
    public ResponseEntity<?> finalizeSupplyChain(@PathVariable Long id) {
        try {
            // Check if already finalized
            boolean isFinalized = finalizationService.isSupplyChainFinalized(id);
            if (isFinalized) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Supply chain is already finalized"));
            }

            // Check for failed blockchain status - can't finalize a chain that failed blockchain creation
            Chains chain = chainService.getSupplyChainById(id);
            if ("FAILED".equals(chain.getBlockchainStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Cannot finalize a supply chain with failed blockchain status. Please retry blockchain creation first."));
            }

            // Only allow finalization for chains with CONFIRMED blockchain status
            if (!"CONFIRMED".equals(chain.getBlockchainStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Supply chain must have CONFIRMED blockchain status before finalization."));
            }

            // Finalize supply chain
            Chains finalizedChain = finalizationService.finalizeSupplyChain(id);

            // Return the complete chain response with nodes and edges
            ChainResponseDTO response = new ChainResponseDTO(
                    finalizedChain.getId(),
                    finalizedChain.getName(),
                    finalizedChain.getDescription(),
                    finalizedChain.getCreatedBy(),
                    finalizedChain.getNodes().stream()
                            .map(NodeResponseDTO::new)
                            .collect(Collectors.toList()),
                    finalizedChain.getEdges().stream()
                            .map(EdgeResponseDTO::new)
                            .collect(Collectors.toList()),
                    finalizedChain.getCreatedAt() != null ? finalizedChain.getCreatedAt().toInstant() : null,
                    finalizedChain.getUpdatedAt() != null ? finalizedChain.getUpdatedAt().toInstant() : null,
                    finalizedChain.getBlockchainStatus(),
                    finalizedChain.getBlockchainTxHash(),
                    finalizedChain.getBlockchainId() // Include blockchain ID in the response
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to finalize supply chain: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/is-finalized")
    public ResponseEntity<?> isFinalized(@PathVariable Long id) {
        try {
            boolean isFinalized = finalizationService.isSupplyChainFinalized(id);
            return ResponseEntity.ok(Map.of("finalized", isFinalized));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check finalization status: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/assigned-users")
    public ResponseEntity<?> getAssignedUsers(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(finalizationService.getAssignedUsers(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get assigned users: " + e.getMessage()));
        }
    }
}