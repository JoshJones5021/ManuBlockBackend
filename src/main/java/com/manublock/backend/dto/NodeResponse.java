package com.manublock.backend.dto;

import com.manublock.backend.models.Nodes;

public class NodeResponse {
    private Long id;
    private String name;
    private String role;
    private double x;
    private double y;
    private String status;
    private Long assignedUserId;

    // ✅ Constructor accepting a `Nodes` object
    public NodeResponse(Nodes node) {
        this.id = node.getId();
        this.name = node.getName();
        this.role = node.getRole();
        this.x = node.getX();
        this.y = node.getY();
        this.status = node.getStatus();
        this.assignedUserId = node.getAssignedUser() != null ? node.getAssignedUser().getId() : null;
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public double getX() { return x; }
    public double getY() { return y; }
    public String getStatus() { return status; }
    public Long getAssignedUserId() { return assignedUserId; }
}
