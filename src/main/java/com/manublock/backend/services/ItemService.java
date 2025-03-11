package com.manublock.backend.services;

import com.manublock.backend.models.Chains;
import com.manublock.backend.models.Items;
import com.manublock.backend.models.Users;
import com.manublock.backend.repositories.ChainRepository;
import com.manublock.backend.repositories.ItemRepository;
import com.manublock.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChainRepository chainRepository;

    @Autowired
    private AdminBlockchainService adminBlockchainService; // Use admin blockchain service instead

    /**
     * Create a new supply chain item in the database and on blockchain
     */
    public Items createItem(Long itemId, String name, String itemType, Long quantity,
                            Long ownerId, Long supplyChainId) {

        Users owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Chains supplyChain = chainRepository.findById(supplyChainId)
                .orElseThrow(() -> new RuntimeException("Supply chain not found"));

        // Create item in database
        Items item = new Items();
        item.setId(itemId);
        item.setName(name);
        item.setItemType(itemType);
        item.setQuantity(quantity);
        item.setOwner(owner);
        item.setSupplyChain(supplyChain);
        item.setStatus("CREATED");
        item.setParentItemIds(new ArrayList<>());
        item.setBlockchainStatus("PENDING");
        item.setCreatedAt(new Date());
        item.setUpdatedAt(new Date());

        // Save to database first
        Items savedItem = itemRepository.save(item);

        // Create item on blockchain using admin wallet
        adminBlockchainService.createItem(itemId, supplyChainId, quantity, itemType, ownerId)
                .thenAccept(txHash -> {
                    // Update blockchain status once transaction is confirmed
                    savedItem.setBlockchainTxHash(txHash);
                    savedItem.setBlockchainStatus("CONFIRMED");
                    itemRepository.save(savedItem);
                })
                .exceptionally(ex -> {
                    // Mark as failed if blockchain transaction fails
                    savedItem.setBlockchainStatus("FAILED");
                    itemRepository.save(savedItem);
                    System.err.println("Failed to create item on blockchain: " + ex.getMessage());
                    return null;
                });

        return savedItem;
    }

    /**
     * Transfer an item from one user to another
     */
    public Items transferItem(Long itemId, Long toUserId, Long quantity, String actionType) {
        Items item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        Long fromUserId = item.getOwner().getId();

        Users recipient = userRepository.findById(toUserId)
                .orElseThrow(() -> new RuntimeException("Recipient user not found"));

        if (quantity > item.getQuantity()) {
            throw new RuntimeException("Insufficient quantity for transfer");
        }

        // For blockchain transfer, we need the recipient's wallet address
        String recipientWalletAddress = recipient.getWalletAddress();
        if (recipientWalletAddress == null || recipientWalletAddress.isEmpty()) {
            throw new RuntimeException("Recipient does not have a wallet address");
        }

        // Update item status for UI feedback
        item.setStatus("IN_TRANSIT");
        item.setBlockchainStatus("PENDING");
        item.setUpdatedAt(new Date());

        Items savedItem = itemRepository.save(item);

        // Perform blockchain transfer using admin wallet
        adminBlockchainService.transferItem(itemId, recipientWalletAddress, quantity, actionType, fromUserId)
                .thenAccept(txHash -> {
                    // Update status after blockchain confirmation
                    if (quantity.equals(item.getQuantity())) {
                        // Full transfer
                        item.setOwner(recipient);
                        item.setBlockchainTxHash(txHash);
                        item.setBlockchainStatus("CONFIRMED");
                        itemRepository.save(item);
                    } else {
                        // Partial transfer - create new item for recipient
                        Long newItemId = itemId * 10000 + System.currentTimeMillis() % 10000;

                        Items newItem = new Items();
                        newItem.setId(newItemId);
                        newItem.setName(item.getName());
                        newItem.setItemType(item.getItemType());
                        newItem.setQuantity(quantity);
                        newItem.setOwner(recipient);
                        newItem.setSupplyChain(item.getSupplyChain());
                        newItem.setStatus("IN_TRANSIT");
                        newItem.setParentItemIds(item.getParentItemIds());
                        newItem.setBlockchainTxHash(txHash);
                        newItem.setBlockchainStatus("CONFIRMED");
                        newItem.setCreatedAt(new Date());
                        newItem.setUpdatedAt(new Date());

                        // Reduce quantity of original item
                        item.setQuantity(item.getQuantity() - quantity);
                        item.setBlockchainStatus("CONFIRMED");

                        itemRepository.save(newItem);
                        itemRepository.save(item);
                    }
                })
                .exceptionally(ex -> {
                    // Revert to original status if blockchain transaction fails
                    item.setStatus("CREATED");
                    item.setBlockchainStatus("FAILED");
                    itemRepository.save(item);
                    System.err.println("Failed to transfer item on blockchain: " + ex.getMessage());
                    return null;
                });

        return savedItem;
    }

    /**
     * Process multiple input items to create a new item
     */
    public Items processItems(List<Long> sourceItemIds, Long newItemId, String newItemName,
                              List<Long> inputQuantities, Long outputQuantity, String newItemType, Long ownerId) {

        if (sourceItemIds.size() != inputQuantities.size()) {
            throw new RuntimeException("Source item IDs and quantities must match");
        }

        Users owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify all source items exist and have sufficient quantities
        List<Items> sourceItems = new ArrayList<>();
        Long supplyChainId = null;

        for (int i = 0; i < sourceItemIds.size(); i++) {
            Long sourceId = sourceItemIds.get(i);
            Long quantity = inputQuantities.get(i);

            Items sourceItem = itemRepository.findById(sourceId)
                    .orElseThrow(() -> new RuntimeException("Source item not found: " + sourceId));

            if (!sourceItem.getOwner().getId().equals(ownerId)) {
                throw new RuntimeException("User is not the owner of all source items");
            }

            if (sourceItem.getQuantity() < quantity) {
                throw new RuntimeException("Insufficient quantity for source item: " + sourceId);
            }

            if (supplyChainId == null) {
                supplyChainId = sourceItem.getSupplyChain().getId();
            } else if (!supplyChainId.equals(sourceItem.getSupplyChain().getId())) {
                throw new RuntimeException("All items must be in the same supply chain");
            }

            sourceItems.add(sourceItem);
        }

        // Create the new processed item
        Items newItem = new Items();
        newItem.setId(newItemId);
        newItem.setName(newItemName);
        newItem.setItemType(newItemType);
        newItem.setQuantity(outputQuantity);
        newItem.setOwner(owner);
        newItem.setSupplyChain(sourceItems.get(0).getSupplyChain());
        newItem.setStatus("CREATED");
        newItem.setParentItemIds(sourceItemIds);
        newItem.setBlockchainStatus("PENDING");
        newItem.setCreatedAt(new Date());
        newItem.setUpdatedAt(new Date());

        // Save to database first
        Items savedNewItem = itemRepository.save(newItem);

        // Process on blockchain using admin wallet
        adminBlockchainService.processItem(sourceItemIds, newItemId, inputQuantities, outputQuantity, newItemType, ownerId)
                .thenAccept(txHash -> {
                    // Update all items after blockchain confirmation
                    savedNewItem.setBlockchainTxHash(txHash);
                    savedNewItem.setBlockchainStatus("CONFIRMED");
                    itemRepository.save(savedNewItem);

                    // Update source items
                    for (int i = 0; i < sourceItems.size(); i++) {
                        Items sourceItem = sourceItems.get(i);
                        Long quantity = inputQuantities.get(i);

                        sourceItem.setQuantity(sourceItem.getQuantity() - quantity);
                        if (sourceItem.getQuantity() == 0) {
                            sourceItem.setStatus("COMPLETED");
                        }
                        itemRepository.save(sourceItem);
                    }
                })
                .exceptionally(ex -> {
                    // Mark as failed if blockchain transaction fails
                    savedNewItem.setBlockchainStatus("FAILED");
                    itemRepository.save(savedNewItem);
                    System.err.println("Failed to process items on blockchain: " + ex.getMessage());
                    return null;
                });

        return savedNewItem;
    }

    /**
     * Update an item's status
     */
    public Items updateItemStatus(Long itemId, String newStatus) {
        Items item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        Long ownerId = item.getOwner().getId();

        // Map string status to blockchain enum value
        Integer blockchainStatus;
        switch (newStatus) {
            case "CREATED" -> blockchainStatus = 0;
            case "IN_TRANSIT" -> blockchainStatus = 1;
            case "PROCESSING" -> blockchainStatus = 2;
            case "COMPLETED" -> blockchainStatus = 3;
            case "REJECTED" -> blockchainStatus = 4;
            default -> throw new RuntimeException("Invalid status: " + newStatus);
        }

        // Update in database
        item.setStatus(newStatus);
        item.setBlockchainStatus("PENDING");
        item.setUpdatedAt(new Date());

        Items savedItem = itemRepository.save(item);

        // Update on blockchain using admin wallet
        adminBlockchainService.updateItemStatus(itemId, blockchainStatus, ownerId)
                .thenAccept(txHash -> {
                    // Update after blockchain confirmation
                    savedItem.setBlockchainTxHash(txHash);
                    savedItem.setBlockchainStatus("CONFIRMED");
                    itemRepository.save(savedItem);
                })
                .exceptionally(ex -> {
                    // Revert if blockchain update fails
                    savedItem.setBlockchainStatus("FAILED");
                    itemRepository.save(savedItem);
                    System.err.println("Failed to update item status on blockchain: " + ex.getMessage());
                    return null;
                });

        return savedItem;
    }

    /**
     * Get an item by its ID
     */
    public Items getItemById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found with ID: " + itemId));
    }

    /**
     * Get all items in a supply chain
     */
    public List<Items> getItemsBySupplyChain(Long supplyChainId) {
        return itemRepository.findBySupplyChain_Id(supplyChainId);
    }

    /**
     * Get all items owned by a user
     */
    public List<Items> getItemsByOwner(Long ownerId) {
        return itemRepository.findByOwner_Id(ownerId);
    }
}