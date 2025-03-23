package com.manublock.backend.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "chains")
public class Chains {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private Users createdBy; // Reference to Users

    @OneToMany(mappedBy = "supplyChain", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference(value = "chain-node")
    private List<Nodes> nodes;

    @OneToMany(mappedBy = "supplyChain", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference(value = "chain-edge")
    private List<Edges> edges;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    // Blockchain-related fields
    @Column
    private String blockchainTxHash;

    @Column
    private String blockchainStatus;

    // New field to store blockchain ID separately from database ID
    @Column
    private Long blockchainId;

    // Original getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Users getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Users createdBy) {
        this.createdBy = createdBy;
    }

    public List<Nodes> getNodes() {
        return nodes;
    }

    public void setNodes(List<Nodes> nodes) {
        this.nodes = nodes;
    }

    public List<Edges> getEdges() {
        return edges;
    }

    public void setEdges(List<Edges> edges) {
        this.edges = edges;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Blockchain-related getters and setters
    public String getBlockchainTxHash() {
        return blockchainTxHash;
    }

    public void setBlockchainTxHash(String blockchainTxHash) {
        this.blockchainTxHash = blockchainTxHash;
    }

    public String getBlockchainStatus() {
        return blockchainStatus;
    }

    public void setBlockchainStatus(String blockchainStatus) {
        this.blockchainStatus = blockchainStatus;
    }

    // Getter and setter for the new blockchainId field
    public Long getBlockchainId() {
        return blockchainId;
    }

    public void setBlockchainId(Long blockchainId) {
        this.blockchainId = blockchainId;
    }
}