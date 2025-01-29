package com.manublock.backend.models;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class SupplyChain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @OneToMany(mappedBy = "supplyChain", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupplyChainNode> nodes;

    @OneToMany(mappedBy = "supplyChain", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Edge> edges;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<SupplyChainNode> getNodes() { return nodes; }
    public void setNodes(List<SupplyChainNode> nodes) { this.nodes = nodes; }

    public List<Edge> getEdges() { return edges; }
    public void setEdges(List<Edge> edges) { this.edges = edges; }
}
