package com.manublock.backend.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "nodes")
public class Nodes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name = "Unnamed Node";

    @Column(nullable = false)
    private String role = "Unassigned";

    @ManyToOne
    @JoinColumn(name = "assigned_user", nullable = true)
    @JsonBackReference(value = "user-node") // ✅ Prevent infinite recursion
    private Users assignedUser;

    @Column(nullable = false)
    private double x = 100;

    @Column(nullable = false)
    private double y = 100;

    @Column(nullable = false)
    private String status = "pending";

    @ManyToOne
    @JoinColumn(name = "supply_chain_id", nullable = false)
    @JsonBackReference(value = "chain-node")
    private Chains supplyChain;

    // ✅ NEW: Getter for assignedUser
    public Users getAssignedUser() {
        return assignedUser;
    }

    // ✅ NEW: Setter for assignedUser as an ID
    public void setAssignedUserId(Long userId) {
        if (userId != null) {
            this.assignedUser = new Users();
            this.assignedUser.setId(userId);
        } else {
            this.assignedUser = null;
        }
    }

    // ✅ NEW: Getter to return only the assigned user ID
    public Long getAssignedUserId() {
        return (assignedUser != null) ? assignedUser.getId() : null;
    }

    // ✅ Modify existing setter for assignedUser object
    public void setAssignedUser(Users assignedUser) {
        this.assignedUser = assignedUser;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Chains getSupplyChain() { return supplyChain; }
    public void setSupplyChain(Chains supplyChain) { this.supplyChain = supplyChain; }
}
