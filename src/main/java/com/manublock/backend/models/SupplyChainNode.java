package com.manublock.backend.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
public class SupplyChainNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name = "Unnamed Node";

    @Column(nullable = false)
    private String role = "Unassigned";

    @Column(nullable = false)
    private Long assignedUser = 0L; // Initialize to 0 or any default value

    @Column(nullable = false)
    private double x = 100;  // Default X position

    @Column(nullable = false)
    private double y = 100;  // Default Y position

    @Column(nullable = false)
    private String status = "pending";

    @ManyToOne
    @JoinColumn(name = "supply_chain_id", nullable = false)
    @JsonBackReference
    private SupplyChain supplyChain;

    // Getters and setters
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public SupplyChain getSupplyChain() { return supplyChain; }
    public void setSupplyChain(SupplyChain supplyChain) { this.supplyChain = supplyChain; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getAssignedUser() { return assignedUser; }
    public void setAssignedUser(Long assignedUser) { this.assignedUser = assignedUser; }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}