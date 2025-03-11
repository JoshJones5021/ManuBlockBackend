package com.manublock.backend.dto;

import com.manublock.backend.models.Users;
import java.time.Instant;
import java.util.List;

public class ChainResponse {
    private Long id;
    private String name;
    private String description;
    private UserResponse createdBy;
    private List<NodeResponse> nodes;
    private List<EdgeResponse> edges;
    private Instant createdAt;
    private Instant updatedAt;
    private String blockchainStatus;
    private String blockchainTxHash;

    public ChainResponse(Long id, String name, String description, Users createdBy,
                         List<NodeResponse> nodes, List<EdgeResponse> edges,
                         Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdBy = new UserResponse(createdBy);
        this.nodes = nodes;
        this.edges = edges;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.blockchainStatus = null; // Default values for backward compatibility
        this.blockchainTxHash = null;
    }

    public ChainResponse(Long id, String name, String description, Users createdBy,
                         List<NodeResponse> nodes, List<EdgeResponse> edges,
                         Instant createdAt, Instant updatedAt,
                         String blockchainStatus, String blockchainTxHash) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdBy = new UserResponse(createdBy);
        this.nodes = nodes;
        this.edges = edges;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.blockchainStatus = blockchainStatus;
        this.blockchainTxHash = blockchainTxHash;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public UserResponse getCreatedBy() { return createdBy; }
    public List<NodeResponse> getNodes() { return nodes; }
    public List<EdgeResponse> getEdges() { return edges; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getBlockchainStatus() { return blockchainStatus; }
    public String getBlockchainTxHash() { return blockchainTxHash; }
}