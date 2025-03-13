package com.manublock.backend.services;

import com.manublock.backend.dto.ChainResponseDTO;
import com.manublock.backend.dto.NodeResponseDTO;
import com.manublock.backend.dto.EdgeResponseDTO;
import com.manublock.backend.models.Edges;
import com.manublock.backend.models.Chains;
import com.manublock.backend.models.Nodes;
import com.manublock.backend.models.Users;
import com.manublock.backend.repositories.EdgeRepository;
import com.manublock.backend.repositories.NodeRepository;
import com.manublock.backend.repositories.ChainRepository;
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
    private static final int MAX_BLOCKCHAIN_RETRIES = 3;

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
     * Improved to better handle blockchain failures
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

        // Async blockchain registration with better error handling
        registerSupplyChainOnBlockchain(savedChain);

        return savedChain;
    }

    /**
     * Registers a supply chain on the blockchain asynchronously
     * With improved error handling and retry mechanism
     */
    private void registerSupplyChainOnBlockchain(Chains chain) {
        try {
            if (blockchainService != null) {
                CompletableFuture<String> future = blockchainService.createSupplyChain(chain.getId());

                // Handle the future completion separately to avoid blocking
                future.thenAccept(txHash -> {
                    try {
                        // Update in a new transaction
                        chain.setBlockchainTxHash(txHash);
                        chain.setBlockchainStatus("CONFIRMED");
                        chainRepository.save(chain);
                        LOGGER.info("Supply chain " + chain.getId() + " successfully registered on blockchain: " + txHash);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error updating blockchain status for chain " + chain.getId(), e);
                    }
                }).exceptionally(ex -> {
                    try {
                        chain.setBlockchainStatus("FAILED");
                        chainRepository.save(chain);
                        LOGGER.log(Level.SEVERE, "Blockchain transaction failed for chain " + chain.getId(), ex);
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
     * Retry a failed blockchain registration
     */
    public boolean retryBlockchainRegistration(Long chainId) {
        Chains chain = chainRepository.findById(chainId)
                .orElseThrow(() -> new RuntimeException("Supply chain not found"));

        if (!"FAILED".equals(chain.getBlockchainStatus())) {
            LOGGER.warning("Cannot retry blockchain registration for chain " + chainId +
                    " with status " + chain.getBlockchainStatus());
            return false;
        }

        chain.setBlockchainStatus("PENDING");
        chainRepository.save(chain);

        registerSupplyChainOnBlockchain(chain);
        return true;
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
     * Update a supply chain - only if not finalized
     */
    public Chains updateSupplyChain(Long id, Chains updatedChains) {
        Chains existingChains = chainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supply Chain not found"));

        // Check if chain is finalized
        if ("FINALIZED".equals(existingChains.getBlockchainStatus())) {
            throw new RuntimeException("Cannot update a finalized supply chain");
        }

        // Check if chain has failed blockchain status
        if ("FAILED".equals(existingChains.getBlockchainStatus())) {
            throw new RuntimeException("Cannot update a supply chain with failed blockchain status");
        }

        if (updatedChains.getName() != null) {
            existingChains.setName(updatedChains.getName());
        }

        if (updatedChains.getDescription() != null) {
            existingChains.setDescription(updatedChains.getDescription());
        }

        if (updatedChains.getNodes() != null) {
            for (Nodes updatedNode : updatedChains.getNodes()) {
                if (updatedNode.getId() == null) {
                    updatedNode.setSupplyChain(existingChains);
                    if (updatedNode.getAssignedUser() != null) {
                        Users user = userRepository.findById(updatedNode.getAssignedUser().getId())
                                .orElseThrow(() -> new RuntimeException("User not found"));
                        updatedNode.setAssignedUser(user);
                    }
                    // Force default status for new nodes
                    updatedNode.setStatus("pending");
                    nodeRepository.save(updatedNode);
                } else {
                    Nodes existingNode = nodeRepository.findById(updatedNode.getId())
                            .orElseThrow(() -> new RuntimeException("Node not found"));
                    existingNode.setName(updatedNode.getName());
                    existingNode.setRole(updatedNode.getRole());
                    // Do not update status from here - only NodeStatusService should change node status
                    existingNode.setX(updatedNode.getX());
                    existingNode.setY(updatedNode.getY());
                    if (updatedNode.getAssignedUser() != null) {
                        Users user = userRepository.findById(updatedNode.getAssignedUser().getId())
                                .orElseThrow(() -> new RuntimeException("User not found"));
                        existingNode.setAssignedUser(user);
                    } else {
                        existingNode.setAssignedUser(null);
                    }
                    nodeRepository.save(existingNode);
                }
            }
        }

        if (updatedChains.getEdges() != null) {
            for (Edges updatedEdge : updatedChains.getEdges()) {
                Nodes sourceNode = nodeRepository.findById(updatedEdge.getSource().getId())
                        .orElseThrow(() -> new RuntimeException("Source Node not found"));

                Nodes targetNode = nodeRepository.findById(updatedEdge.getTarget().getId())
                        .orElseThrow(() -> new RuntimeException("Target Node not found"));

                if (updatedEdge.getId() == null) {
                    updatedEdge.setSupplyChain(existingChains);
                    updatedEdge.setSource(sourceNode);
                    updatedEdge.setTarget(targetNode);
                    edgeRepository.save(updatedEdge);
                } else {
                    Edges existingEdge = edgeRepository.findById(updatedEdge.getId())
                            .orElseThrow(() -> new RuntimeException("Edge not found"));

                    existingEdge.setSource(sourceNode);
                    existingEdge.setTarget(targetNode);
                    existingEdge.setAnimated(updatedEdge.getAnimated());
                    existingEdge.setStrokeColor(updatedEdge.getStrokeColor());
                    existingEdge.setStrokeWidth(updatedEdge.getStrokeWidth());

                    edgeRepository.save(existingEdge);
                }
            }
        }

        existingChains.setUpdatedAt(new Date());
        return chainRepository.save(existingChains);
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