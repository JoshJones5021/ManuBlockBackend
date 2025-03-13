package com.manublock.backend.dto;

import com.manublock.backend.models.Users;
import java.time.Instant;
import java.util.List;

public class ChainResponseDTO {
    private Long id;
    private String name;
    private String description;
    private UserResponseDTO createdBy;
    private List<NodeResponseDTO> nodes;
    private List<EdgeResponseDTO> edges;
    private Instant createdAt;
    private Instant updatedAt;
    private String blockchainStatus;
    private String blockchainTxHash;

    public ChainResponseDTO(Long id, String name, String description, Users createdBy,
                            List<NodeResponseDTO> nodes, List<EdgeResponseDTO> edges,
                            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdBy = new UserResponseDTO(createdBy);
        this.nodes = nodes;
        this.edges = edges;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.blockchainStatus = null; // Default values for backward compatibility
        this.blockchainTxHash = null;
    }

    public ChainResponseDTO(Long id, String name, String description, Users createdBy,
                            List<NodeResponseDTO> nodes, List<EdgeResponseDTO> edges,
                            Instant createdAt, Instant updatedAt,
                            String blockchainStatus, String blockchainTxHash) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdBy = new UserResponseDTO(createdBy);
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
    public UserResponseDTO getCreatedBy() { return createdBy; }
    public List<NodeResponseDTO> getNodes() { return nodes; }
    public List<EdgeResponseDTO> getEdges() { return edges; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getBlockchainStatus() { return blockchainStatus; }
    public String getBlockchainTxHash() { return blockchainTxHash; }
}