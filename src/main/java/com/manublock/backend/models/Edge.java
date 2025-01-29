package com.manublock.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.manublock.backend.models.SupplyChain;
import jakarta.persistence.*;

@Entity
public class Edge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;
    private String target;
    private Boolean animated;
    private String strokeColor;
    private Integer strokeWidth;

    @ManyToOne
    @JoinColumn(name = "supply_chain_id", nullable = false)
    @JsonIgnore // 🔴 Prevents infinite recursion
    private SupplyChain supplyChain;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public Boolean getAnimated() { return animated; }
    public void setAnimated(Boolean animated) { this.animated = animated; }

    public String getStrokeColor() { return strokeColor; }
    public void setStrokeColor(String strokeColor) { this.strokeColor = strokeColor; }

    public Integer getStrokeWidth() { return strokeWidth; }
    public void setStrokeWidth(Integer strokeWidth) { this.strokeWidth = strokeWidth; }

    public SupplyChain getSupplyChain() { return supplyChain; }

    // 🔥 **Add this missing method**
    public void setSupplyChain(SupplyChain supplyChain) {
        this.supplyChain = supplyChain;
    }
}
