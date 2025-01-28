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

    // ✅ Add Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {  // ✅ Add this method
        return name;
    }

    public void setName(String name) {  // ✅ Add this method
        this.name = name;
    }

    public String getDescription() {  // ✅ Add this method
        return description;
    }

    public void setDescription(String description) {  // ✅ Add this method
        this.description = description;
    }

    public List<SupplyChainNode> getNodes() {  // ✅ Add this method
        return nodes;
    }

    public void setNodes(List<SupplyChainNode> nodes) {  // ✅ Add this method
        this.nodes = nodes;
    }
}
