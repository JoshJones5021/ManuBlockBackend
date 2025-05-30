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
     * Using a random blockchain ID to avoid ID conflicts
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

        // Generate a unique random blockchain ID
        Long blockchainId = generateUniqueBlockchainId();

        // Async blockchain registration with random ID
        registerSupplyChainOnBlockchainWithRandomId(savedChain, blockchainId);

        return savedChain;
    }

    /**
     * Generates a unique ID for blockchain that is unrelated to database IDs
     * to avoid conflicts with existing blockchain IDs
     */
    private Long generateUniqueBlockchainId() {
        // Generate a random ID in a very large range to minimize collision probability
        // Using current timestamp as a base and adding random component
        long timestamp = System.currentTimeMillis();
        int randomComponent = new Random().nextInt(1000000);
        return timestamp * 1000L + randomComponent;
    }

    /**
     * Registers a supply chain on the blockchain using a random ID
     * instead of trying to use the database ID
     */
    private void registerSupplyChainOnBlockchainWithRandomId(Chains chain, Long blockchainId) {
        try {
            if (blockchainService != null) {
                LOGGER.info("Attempting blockchain registration for chain " + chain.getId() +
                        " with blockchain ID " + blockchainId);

                CompletableFuture<String> future = blockchainService.createSupplyChain(blockchainId, chain.getCreatedBy().getId());

                future.thenAccept(txHash -> {
                    try {
                        // Store both IDs in the database to maintain the mapping
                        chain.setBlockchainTxHash(txHash);
                        chain.setBlockchainStatus("CONFIRMED");
                        chain.setBlockchainId(blockchainId); // Store the blockchain ID
                        chainRepository.save(chain);
                        LOGGER.info("Supply chain " + chain.getId() + " successfully registered on blockchain with ID: " +
                                blockchainId + ", txHash: " + txHash);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error updating blockchain status for chain " + chain.getId(), e);
                    }
                }).exceptionally(ex -> {
                    try {
                        // Mark as failed if blockchain transaction fails
                        LOGGER.log(Level.SEVERE, "Blockchain transaction failed for chain " + chain.getId(), ex);
                        chain.setBlockchainStatus("FAILED");
                        chain.setBlockchainTxHash(null);
                        chainRepository.save(chain);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error updating blockchain failure status for chain " + chain.getId(), e);
                    }
                    return null;
                });
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
                        chain.getBlockchainStatus(),
                        chain.getBlockchainTxHash(),
                        chain.getBlockchainId()
                ))
                .collect(Collectors.toList());
    }

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
                chain.getBlockchainStatus(),
                chain.getBlockchainTxHash(),
                chain.getBlockchainId()
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
        status.put("blockchainId", chain.getBlockchainId());
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
                chain.getBlockchainTxHash(),
                chain.getBlockchainId()
        )).collect(Collectors.toList());
    }
}