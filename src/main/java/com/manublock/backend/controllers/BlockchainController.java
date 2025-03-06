package com.manublock.backend.controllers;

import com.manublock.backend.services.BlockchainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/blockchain")
public class BlockchainController {

    private final BlockchainService blockchainService;

    @Autowired
    public BlockchainController(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }

    @PostMapping("/supply-chains")
    public CompletableFuture<ResponseEntity<String>> createSupplyChain(@RequestBody Map<String, Long> request) {
        Long supplyChainId = request.get("supplyChainId");

        return blockchainService.createSupplyChain(supplyChainId)
                .thenApply(txHash -> {
                    return ResponseEntity.ok("Supply chain created. Transaction hash: " + txHash);
                });
    }

    // Add other endpoints for blockchain operations
}