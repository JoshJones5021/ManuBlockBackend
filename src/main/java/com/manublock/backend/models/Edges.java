package com.manublock.backend.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "edges")
public class Edges {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source", nullable = false)
    private Nodes source;

    @ManyToOne
    @JoinColumn(name = "target", nullable = false)
    private Nodes target;

    private Boolean animated;
    private String strokeColor;
    private Integer strokeWidth;

    @ManyToOne
    @JoinColumn(name = "supply_chain_id", nullable = false)
    @JsonBackReference(value = "chain-edge")  // ✅ Match with Chains.java
    private Chains supplyChain;

    public Boolean getAnimated() {
        return animated != null ? animated : false;
    }

    public void setAnimated(Boolean animated) {
        this.animated = animated;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Nodes getSource() {
        return source;
    }

    public void setSource(Nodes source) {
        this.source = source;
    }

    public Nodes getTarget() {
        return target;
    }

    public void setTarget(Nodes target) {
        this.target = target;
    }

    public String getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(String strokeColor) {
        this.strokeColor = strokeColor;
    }

    public Integer getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(Integer strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public Chains getSupplyChain() {
        return supplyChain;
    }

    public void setSupplyChain(Chains supplyChain) {
        this.supplyChain = supplyChain;
    }
}
