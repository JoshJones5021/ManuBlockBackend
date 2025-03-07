package com.manublock.backend.services;

import com.manublock.backend.dto.ChainResponse;
import com.manublock.backend.dto.NodeResponse;
import com.manublock.backend.dto.EdgeResponse;
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
import java.util.stream.Collectors;

@Service
public class ChainService {

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

    public Chains createSupplyChain(String name, String description, Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Chains chain = new Chains();
        chain.setName(name);
        chain.setDescription(description);
        chain.setCreatedBy(user);
        chain.setCreatedAt(new Date());
        chain.setUpdatedAt(new Date());
        chain.setNodes(new ArrayList<>());
        chain.setEdges(new ArrayList<>());

        // Set initial blockchain status
        chain.setBlockchainStatus("PENDING");

        // Save to database first
        Chains savedChain = chainRepository.save(chain);

        try {
            // Check if BlockchainService is available and properly configured
            if (blockchainService != null) {
                CompletableFuture<String> future = blockchainService.createSupplyChain(savedChain.getId());

                // Handle the future completion separately to avoid blocking
                future.thenAccept(txHash -> {
                    try {
                        // Update in a new transaction
                        savedChain.setBlockchainTxHash(txHash);
                        savedChain.setBlockchainStatus("CONFIRMED");
                        chainRepository.save(savedChain);
                    } catch (Exception e) {
                        System.err.println("Error updating blockchain status: " + e.getMessage());
                    }
                }).exceptionally(ex -> {
                    try {
                        savedChain.setBlockchainStatus("FAILED");
                        chainRepository.save(savedChain);
                        System.err.println("Blockchain transaction failed: " + ex.getMessage());
                    } catch (Exception e) {
                        System.err.println("Error updating blockchain failure status: " + e.getMessage());
                    }
                    return null;
                });
            }
        } catch (Exception e) {
            // Log the error but don't prevent chain creation
            System.err.println("Error with blockchain integration: " + e.getMessage());
            e.printStackTrace();
        }

        // Return the saved chain regardless of blockchain status
        return savedChain;
    }

    public List<ChainResponse> getAllSupplyChains() {
        return chainRepository.findAll().stream()
                .map(chain -> new ChainResponse(
                        chain.getId(),
                        chain.getName(),
                        chain.getDescription(),
                        chain.getCreatedBy(),  // ✅ Pass the Users object, which will be converted to UserResponse in ChainResponse
                        chain.getNodes().stream()
                                .map(NodeResponse::new)  // ✅ Correctly calls the constructor that takes a `Nodes` object
                                .collect(Collectors.toList()),
                        chain.getEdges().stream()
                                .map(EdgeResponse::new)  // ✅ This correctly calls the constructor that takes an `Edges` object
                                .collect(Collectors.toList()),
                        chain.getCreatedAt() != null ? chain.getCreatedAt().toInstant() : null,
                        chain.getUpdatedAt() != null ? chain.getUpdatedAt().toInstant() : null
                ))
                .collect(Collectors.toList());
    }

    public ChainResponse getSupplyChain(Long id) {
        Chains chain = chainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supply Chain not found"));

        return new ChainResponse(
                chain.getId(),
                chain.getName(),
                chain.getDescription(),
                chain.getCreatedBy(),  // ✅ Pass the Users object, which will be converted to UserResponse in ChainResponse
                chain.getNodes().stream()
                        .map(NodeResponse::new)  // ✅ Calls the constructor that takes a Nodes object
                        .collect(Collectors.toList()),

                chain.getEdges().stream()
                        .map(EdgeResponse::new)  // ✅ Calls the constructor that takes an Edges object
                        .collect(Collectors.toList()),
                chain.getCreatedAt() != null ? chain.getCreatedAt().toInstant() : null,
                chain.getUpdatedAt() != null ? chain.getUpdatedAt().toInstant() : null
        );
    }

    public Chains updateSupplyChain(Long id, Chains updatedChains) {
        Chains existingChains = chainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supply Chain not found"));

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
                    nodeRepository.save(updatedNode);
                } else {
                    Nodes existingNode = nodeRepository.findById(updatedNode.getId())
                            .orElseThrow(() -> new RuntimeException("Node not found"));
                    existingNode.setName(updatedNode.getName());
                    existingNode.setRole(updatedNode.getRole());
                    existingNode.setStatus(updatedNode.getStatus());
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

    public void deleteSupplyChain(Long id) {
        Chains chain = chainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supply Chain not found"));
        nodeRepository.deleteAll(chain.getNodes());
        edgeRepository.deleteAll(chain.getEdges());
        chainRepository.delete(chain);
    }

    @Transactional
    public void updateBlockchainInfo(Long chainId, String txHash) {
        Chains chain = chainRepository.findById(chainId)
                .orElseThrow(() -> new RuntimeException("Supply Chain not found"));

        chain.setBlockchainTxHash(txHash);
        chain.setBlockchainStatus("CONFIRMED");
        chain.setUpdatedAt(new Date());

        chainRepository.save(chain);
    }

    public Map<String, Object> getBlockchainStatus(Long chainId) {
        Chains chain = chainRepository.findById(chainId)
                .orElseThrow(() -> new RuntimeException("Supply Chain not found"));

        Map<String, Object> status = new HashMap<>();
        status.put("id", chain.getId());
        status.put("blockchainStatus", chain.getBlockchainStatus());
        status.put("blockchainTxHash", chain.getBlockchainTxHash());

        return status;
    }
}
