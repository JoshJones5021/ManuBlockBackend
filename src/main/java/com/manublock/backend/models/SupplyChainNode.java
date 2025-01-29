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
    private String assignedUser = "Unassigned";

    @Column(nullable = false)
    private double x = 100;  // Default X position

    @Column(nullable = false)
    private double y = 100;  // Default Y position

    @ManyToOne
    @JoinColumn(name = "supply_chain_id", nullable = false)
    @JsonBackReference
    private SupplyChain supplyChain;

    // **✅ Add missing getters and setters for x and y**
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

    public String getAssignedUser() { return assignedUser; }
    public void setAssignedUser(String assignedUser) { this.assignedUser = assignedUser; }
}
