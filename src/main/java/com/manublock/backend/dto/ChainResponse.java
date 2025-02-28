package com.manublock.backend.dto;

import com.manublock.backend.models.Users;
import java.time.Instant;
import java.util.List;

public class ChainResponse {
    private Long id;
    private String name;
    private String description;
    private UserResponse createdBy;  // ✅ Change Long to UserResponse
    private List<NodeResponse> nodes;
    private List<EdgeResponse> edges;
    private Instant createdAt;
    private Instant updatedAt;

    public ChainResponse(Long id, String name, String description, Users createdBy,  // ✅ Accept Users object
                         List<NodeResponse> nodes, List<EdgeResponse> edges,
                         Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdBy = new UserResponse(createdBy);  // ✅ Convert Users to UserResponse
        this.nodes = nodes;
        this.edges = edges;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public UserResponse getCreatedBy() { return createdBy; }
    public List<NodeResponse> getNodes() { return nodes; }
    public List<EdgeResponse> getEdges() { return edges; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
