package com.manublock.backend.services;

import com.manublock.backend.dto.ChainResponseDTO;
import com.manublock.backend.dto.EdgeResponseDTO;
import com.manublock.backend.dto.NodeResponseDTO;
import com.manublock.backend.models.Chains;
import com.manublock.backend.models.Edges;
import com.manublock.backend.models.Nodes;
import com.manublock.backend.models.Users;
import com.manublock.backend.repositories.ChainRepository;
import com.manublock.backend.repositories.EdgeRepository;
import com.manublock.backend.repositories.NodeRepository;
import com.manublock.backend.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class ChainService {
    private static final Logger LOGGER = Logger.getLogger(ChainService.class.getName());
    private static final int MAX_BLOCKCHAIN_RETRIES = 5;
    private static final int MAX_ID_INCREMENT_RETRIES = 10;

    @Autowired
    private ChainRepository chainRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EdgeRepository edgeRepository;

    @Autowired
    private BlockchainService blockchainService;

    /**
     * Creates a new supply chain in the database and registers it on the blockchain
     * With improved retry mechanism for handling ID conflicts on the blockchain
     */
    public Chains createSupplyChain(String name, String description, Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Create a new supply chain with initial blockchain status as PENDING
        Chains chain = new Chains();
        chain.setName(name);
        chain.setDescription(description);
        chain.setCreatedBy(user);
        chain.setCreatedAt(new Date());
        chain.setUpdatedAt(new Date());
        chain.setNodes(new ArrayList<>());
        chain.setEdges(new ArrayList<>());
        chain.setBlockchainStatus("PENDING");

        // Save to database first
        Chains savedChain = chainRepository.save(chain);
        LOGGER.info("Created supply chain in database with ID: " + savedChain.getId());

        // Async blockchain registration with improved error handling and retry mechanism
        registerSupplyChainOnBlockchainWithRetry(savedChain);

        return savedChain;
    }

    /**
     * Registers a supply chain on the blockchain asynchronously
     * With improved error handling, retry mechanism, and ID conflict resolution
     */
    private void registerSupplyChainOnBlockchainWithRetry(Chains chain) {
        try {
            if (blockchainService != null) {
                attemptBlockchainRegistration(chain.getId(), chain.getId(), 0, chain);
            } else {
                LOGGER.warning("BlockchainService not available - unable to register chain " + chain.getId());
                chain.setBlockchainStatus("PENDING");
                chainRepository.save(chain);
            }
        } catch (Exception e) {
            // Log the error but don't prevent chain creation
            LOGGER.log(Level.SEVERE, "Error with blockchain integration for chain " + chain.getId(), e);
            chain.setBlockchainStatus("FAILED");
            chainRepository.save(chain);
        }
    }

    /**
     * Attempts to register a supply chain on the blockchain, with retry logic
     * for handling ID conflicts and other errors
     *
     * @param originalId The original database ID of the chain
     * @param attemptId The current ID we're trying to use on the blockchain
     * @param retryCount Current retry count
     * @param chain The chain entity to update with results
     */
    private void attemptBlockchainRegistration(Long originalId, Long attemptId, int retryCount, Chains chain) {
        if (retryCount >= MAX_ID_INCREMENT_RETRIES) {
            LOGGER.severe("Exceeded maximum ID increment retries for chain " + originalId + ". Giving up.");
            chain.setBlockchainStatus("FAILED");
            chain.setBlockchainTxHash("ID_CONFLICT_UNRESOLVABLE");
            chainRepository.save(chain);
            return;
        }

        LOGGER.info("Attempting blockchain registration for chain " + originalId +
                " with blockchain ID " + attemptId + " (attempt #" + (retryCount + 1) + ")");

        CompletableFuture<String> future = blockchainService.createSupplyChain(attemptId, chain.getCreatedBy().getId());

        future.thenAccept(txHash -> {
            try {
                // Registration succeeded - update with successful transaction
                if (originalId.equals(attemptId)) {
                    // Original ID worked - simple update
                    chain.setBlockchainTxHash(txHash);
                    chain.setBlockchainStatus("CONFIRMED");
                    chainRepository.save(chain);
                    LOGGER.info("Supply chain " + chain.getId() + " successfully registered on blockchain with original ID: " + txHash);
                } else {
                    // We used a different ID on the blockchain than in our database
                    // Log this mapping for future reference
                    chain.setBlockchainTxHash(txHash);
                    chain.setBlockchainStatus("CONFIRMED");
                    // Store the mapping in some way - either in the hash field or in a separate table
                    // Here we'll store it in the tx hash with a prefix
                    chain.setBlockchainTxHash("REMAPPED:" + attemptId + ":" + txHash);
                    chainRepository.save(chain);
                    LOGGER.info("Supply chain " + chain.getId() + " registered on blockchain with remapped ID " +
                            attemptId + ": " + txHash);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error updating blockchain status for chain " + chain.getId(), e);
            }
        }).exceptionally(ex -> {
            if (ex.getMessage().contains("already exists") ||
                    ex.getMessage().contains("duplicate") ||
                    ex.getMessage().contains("ID conflict")) {
                // ID conflict detected - try with incremented ID
                Long nextAttemptId = attemptId + 1;
                LOGGER.warning("ID conflict detected for chain ID " + attemptId + ". Retrying with ID " + nextAttemptId);

                // Wait a small amount of time before retrying
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                // Recursive call with incremented ID
                attemptBlockchainRegistration(originalId, nextAttemptId, retryCount + 1, chain);
            } else {
                try {
                    // For other types of errors, mark as failed
                    LOGGER.log(Level.SEVERE, "Blockchain transaction failed for chain " + chain.getId(), ex);
                    chain.setBlockchainStatus("FAILED");
                    chain.setBlockchainTxHash(null);
                    chainRepository.save(chain);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error updating blockchain failure status for chain " + chain.getId(), e);
                }
            }
            return null;
        });
    }

    /**
     * Get all supply chains with their blockchain status
     */
    public List<ChainResponseDTO> getAllSupplyChains() {
        return chainRepository.findAll().stream()
                .map(chain -> new ChainResponseDTO(
                        chain.getId(),
                        chain.getName(),
                        chain.getDescription(),
                        chain.getCreatedBy(),
                        chain.getNodes().stream()
                                .map(NodeResponseDTO::new)
                                .collect(Collectors.toList()),
                        chain.getEdges().stream()
                                .map(EdgeResponseDTO::new)
                                .collect(Collectors.toList()),
                        chain.getCreatedAt() != null ? chain.getCreatedAt().toInstant() : null,
                        chain.getUpdatedAt() != null ? chain.getUpdatedAt().toInstant() : null,
                        chain.getBlockchainStatus(), // Include blockchain status
                        chain.getBlockchainTxHash() // Include blockchain transaction hash
                ))
                .collect(Collectors.toList());
    }

    // Rest of the methods remain unchanged...
    /**
     * Get a specific supply chain by ID with blockchain status
     */
    @Transactional
    public ChainResponseDTO getSupplyChain(Long id) {
        Chains chain = chainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supply Chain not found"));

        return new ChainResponseDTO(
                chain.getId(),
                chain.getName(),
                chain.getDescription(),
                chain.getCreatedBy(),
                chain.getNodes().stream()
                        .map(NodeResponseDTO::new)
                        .collect(Collectors.toList()),
                chain.getEdges().stream()
                        .map(EdgeResponseDTO::new)
                        .collect(Collectors.toList()),
                chain.getCreatedAt() != null ? chain.getCreatedAt().toInstant() : null,
                chain.getUpdatedAt() != null ? chain.getUpdatedAt().toInstant() : null,
                chain.getBlockchainStatus(), // Include blockchain status
                chain.getBlockchainTxHash() // Include blockchain transaction hash
        );
    }

    /**
     * Get a supply chain entity by ID
     */
    public Chains getSupplyChainById(Long id) {
        return chainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supply Chain not found"));
    }

    /**
     * Delete a supply chain - only if not finalized and has failed blockchain status
     */
    public void deleteSupplyChain(Long id) {
        Chains chain = chainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supply Chain not found"));

        // Check if chain is finalized
        if ("FINALIZED".equals(chain.getBlockchainStatus())) {
            throw new RuntimeException("Cannot delete a finalized supply chain");
        }

        // If chain is confirmed on blockchain but not finalized, it shouldn't be deleted
        if ("CONFIRMED".equals(chain.getBlockchainStatus())) {
            throw new RuntimeException("Cannot delete a supply chain that is registered on the blockchain");
        }

        // Only delete if chain has FAILED or PENDING blockchain status
        nodeRepository.deleteAll(chain.getNodes());
        edgeRepository.deleteAll(chain.getEdges());
        chainRepository.delete(chain);
        LOGGER.info("Deleted supply chain with ID: " + id);
    }

    @Transactional
    public void updateBlockchainInfo(Long chainId, String txHash) {
        Chains chain = chainRepository.findById(chainId)
                .orElseThrow(() -> new RuntimeException("Supply Chain not found"));

        chain.setBlockchainTxHash(txHash);
        chain.setBlockchainStatus("CONFIRMED");
        chain.setUpdatedAt(new Date());

        chainRepository.save(chain);
        LOGGER.info("Updated blockchain status for chain " + chainId + " to CONFIRMED with hash: " + txHash);
    }

    public Map<String, Object> getBlockchainStatus(Long chainId) {
        Chains chain = chainRepository.findById(chainId)
                .orElseThrow(() -> new RuntimeException("Supply Chain not found"));

        Map<String, Object> status = new HashMap<>();
        status.put("id", chain.getId());
        status.put("blockchainStatus", chain.getBlockchainStatus());
        status.put("blockchainTxHash", chain.getBlockchainTxHash());
        status.put("name", chain.getName());
        status.put("updatedAt", chain.getUpdatedAt());

        return status;
    }

    public List<ChainResponseDTO> findSupplyChainsByUserId(Long userId) {
        List<Chains> userChains = chainRepository.findChainsByAssignedUser(userId);

        return userChains.stream().map(chain -> new ChainResponseDTO(
                chain.getId(),
                chain.getName(),
                chain.getDescription(),
                chain.getCreatedBy(),
                chain.getNodes().stream().map(NodeResponseDTO::new).collect(Collectors.toList()),
                chain.getEdges().stream().map(EdgeResponseDTO::new).collect(Collectors.toList()),
                chain.getCreatedAt() != null ? chain.getCreatedAt().toInstant() : null,
                chain.getUpdatedAt() != null ? chain.getUpdatedAt().toInstant() : null,
                chain.getBlockchainStatus(),
                chain.getBlockchainTxHash()
        )).collect(Collectors.toList());
    }
}