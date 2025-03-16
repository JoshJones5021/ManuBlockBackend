package com.manublock.backend.controllers;

import com.manublock.backend.services.ExtendedBlockchainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/blockchain/items")
public class ItemController {

    private final ExtendedBlockchainService blockchainService;

    @Autowired
    public ItemController(ExtendedBlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }

    @PostMapping("/create")
    public CompletableFuture<ResponseEntity<String>> createItem(@RequestBody Map<String, Object> request) {
        try {
            Long itemId = ((Number) request.get("itemId")).longValue();
            Long supplyChainId = ((Number) request.get("supplyChainId")).longValue();
            Long quantity = ((Number) request.get("quantity")).longValue();
            String itemType = (String) request.get("itemType");
            Long creatorId = ((Number) request.get("creatorId")).longValue(); // Added creatorId

            return blockchainService.createItem(itemId, supplyChainId, quantity, itemType, creatorId)
                    .thenApply(txHash -> ResponseEntity.ok("Item created. Transaction hash: " + txHash))
                    .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error creating item: " + ex.getMessage()));
        } catch (Exception e) {
            CompletableFuture<ResponseEntity<String>> future = new CompletableFuture<>();
            future.complete(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid request parameters: " + e.getMessage()));
            return future;
        }
    }

    @PostMapping("/transfer")
    public CompletableFuture<ResponseEntity<String>> transferItem(@RequestBody Map<String, Object> request) {
        try {
            Long itemId = ((Number) request.get("itemId")).longValue();
            Long toUserId = ((Number) request.get("toUserId")).longValue(); // Changed from toAddress (String) to toUserId (Long)
            Long quantity = ((Number) request.get("quantity")).longValue();
            String actionType = (String) request.get("actionType");
            Long fromUserId = ((Number) request.get("fromUserId")).longValue(); // Added fromUserId

            return blockchainService.transferItem(itemId, toUserId, quantity, actionType, fromUserId)
                    .thenApply(txHash -> ResponseEntity.ok("Item transferred. Transaction hash: " + txHash))
                    .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error transferring item: " + ex.getMessage()));
        } catch (Exception e) {
            CompletableFuture<ResponseEntity<String>> future = new CompletableFuture<>();
            future.complete(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid request parameters: " + e.getMessage()));
            return future;
        }
    }

    @PostMapping("/process")
    public CompletableFuture<ResponseEntity<String>> processItems(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> sourceItemIdsRaw = (List<Integer>) request.get("sourceItemIds");
            List<Long> sourceItemIds = sourceItemIdsRaw.stream()
                    .map(Integer::longValue)
                    .toList();

            Long newItemId = ((Number) request.get("newItemId")).longValue();

            @SuppressWarnings("unchecked")
            List<Integer> inputQuantitiesRaw = (List<Integer>) request.get("inputQuantities");
            List<Long> inputQuantities = inputQuantitiesRaw.stream()
                    .map(Integer::longValue)
                    .toList();

            Long outputQuantity = ((Number) request.get("outputQuantity")).longValue();
            String newItemType = (String) request.get("newItemType");
            Long processorId = ((Number) request.get("processorId")).longValue(); // Added processorId

            return blockchainService.processItem(sourceItemIds, newItemId, inputQuantities, outputQuantity, newItemType, processorId)
                    .thenApply(txHash -> ResponseEntity.ok("Items processed. Transaction hash: " + txHash))
                    .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error processing items: " + ex.getMessage()));
        } catch (Exception e) {
            CompletableFuture<ResponseEntity<String>> future = new CompletableFuture<>();
            future.complete(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid request parameters: " + e.getMessage()));
            return future;
        }
    }

    @PostMapping("/update-status")
    public CompletableFuture<ResponseEntity<String>> updateItemStatus(@RequestBody Map<String, Object> request) {
        try {
            Long itemId = ((Number) request.get("itemId")).longValue();
            Integer newStatus = ((Number) request.get("newStatus")).intValue();
            Long ownerId = ((Number) request.get("ownerId")).longValue(); // Added ownerId

            return blockchainService.updateItemStatus(itemId, newStatus, ownerId)
                    .thenApply(txHash -> ResponseEntity.ok("Item status updated. Transaction hash: " + txHash))
                    .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error updating item status: " + ex.getMessage()));
        } catch (Exception e) {
            CompletableFuture<ResponseEntity<String>> future = new CompletableFuture<>();
            future.complete(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid request parameters: " + e.getMessage()));
            return future;
        }
    }
}