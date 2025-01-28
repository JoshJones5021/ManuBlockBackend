package com.manublock.backend.models;

import jakarta.persistence.*;

@Entity
public class SupplyChainNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String role;
    private String assignedUser; // Username or wallet address

    @ManyToOne
    @JoinColumn(name = "supply_chain_id")
    private SupplyChain supplyChain;

    // Constructors, Getters, and Setters
}
