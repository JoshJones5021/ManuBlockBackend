package com.manublock.backend.services;

import com.manublock.backend.models.*;
import com.manublock.backend.repositories.BlockchainTransactionRepository;
import com.manublock.backend.repositories.ChainRepository;
import com.manublock.backend.repositories.ItemRepository;
import com.manublock.backend.repositories.UserRepository;
import com.manublock.backend.repositories.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Extended blockchain service that provides higher-level operations
 * for interacting with the blockchain and maintains consistent state
 * in the local database
 */
@Service
public class ExtendedBlockchainService {

    private final BlockchainService blockchainService;
    private final BlockchainTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ChainRepository chainRepository;
    private final ItemRepository itemRepository;
    private final OrderItemRepository orderItemRepository;

    @Autowired
    public ExtendedBlockchainService(
            BlockchainService blockchainService,
            BlockchainTransactionRepository transactionRepository,
            UserRepository userRepository,
            ChainRepository chainRepository,
            ItemRepository itemRepository,
            OrderItemRepository orderItemRepository) {
        this.blockchainService = blockchainService;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.chainRepository = chainRepository;
        this.itemRepository = itemRepository;
        this.orderItemRepository = orderItemRepository;
    }

    /**
     * Authorizes a participant to interact with a supply chain
     * @param supplyChainId The ID of the supply chain
     * @param participantUserId The user ID of the participant to authorize
     * @return CompletableFuture containing the transaction hash
     */
    public CompletableFuture<String> authorizeParticipant(Long supplyChainId, Long participantUserId) {
        // Create a transaction record
        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setFunction("authorizeParticipant");
        tx.setParameters(supplyChainId + "," + participantUserId);
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        tx.setRetryCount(0);
        transactionRepository.save(tx);

        // Get the contract from the blockchain service
        // Changed from walletAddress to participantUserId as BigInteger
        RemoteFunctionCall<TransactionReceipt> functionCall =
                blockchainService.getContract().authorizeParticipant(
                        BigInteger.valueOf(supplyChainId),
                        BigInteger.valueOf(participantUserId));

        // Use the existing sendTransactionWithRetry mechanism
        return blockchainService.sendTransactionWithRetry(functionCall, tx);
    }

    /**
     * Creates a new supply chain item on the blockchain
     * and updates database to maintain consistent state
     */
    public CompletableFuture<String> createItem(
            Long itemId,
            Long supplyChainId,
            Long quantity,
            String itemType,
            Long creatorId) {

        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setFunction("createItem");
        tx.setParameters(itemId + "," + supplyChainId + "," + quantity + "," + itemType + "," + creatorId);
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        tx.setRetryCount(0);
        transactionRepository.save(tx);

        RemoteFunctionCall<TransactionReceipt> functionCall =
                blockchainService.getContract().createItem(
                        BigInteger.valueOf(itemId),
                        BigInteger.valueOf(supplyChainId),
                        BigInteger.valueOf(quantity),
                        itemType,
                        BigInteger.valueOf(creatorId));

        // Send blockchain transaction and update database upon completion
        return blockchainService.sendTransactionWithRetry(functionCall, tx)
                .thenApply(txHash -> {
                    // Create corresponding database entry in Items table
                    try {
                        // Find creator user
                        Optional<Users> creatorOpt = userRepository.findById(creatorId);
                        // Find supply chain
                        Optional<Chains> chainOpt = chainRepository.findById(supplyChainId);

                        if (creatorOpt.isPresent() && chainOpt.isPresent()) {
                            Items newItem = new Items();
                            newItem.setId(itemId);
                            newItem.setName(itemType); // Default name based on type
                            newItem.setItemType(itemType);
                            newItem.setQuantity(quantity);
                            newItem.setOwner(creatorOpt.get());
                            newItem.setSupplyChain(chainOpt.get());
                            newItem.setStatus("CREATED");
                            newItem.setCreatedAt(new Date());
                            newItem.setUpdatedAt(new Date());
                            newItem.setBlockchainTxHash(txHash);
                            newItem.setBlockchainStatus("CONFIRMED");

                            // Initialize empty parent IDs list
                            newItem.setParentItemIds(new ArrayList<>());

                            itemRepository.save(newItem);
                        }
                    } catch (Exception e) {
                        // Log error but don't fail the transaction
                        System.err.println("Error creating database entry after blockchain item creation: " + e.getMessage());
                    }
                    return txHash;
                });
    }

    /**
     * Transfers an item from one participant to another
     * @param itemId ID of the item to transfer
     * @param toUserId Recipient's user ID (not wallet address)
     * @param quantity Amount to transfer
     * @param actionType Description of the transfer action
     * @param fromUserId User ID of the current owner
     * @return CompletableFuture containing the transaction hash
     */
    public CompletableFuture<String> transferItem(
            Long itemId,
            Long toUserId,
            Long quantity,
            String actionType,
            Long fromUserId) {

        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setFunction("transferItem");
        tx.setParameters(itemId + "," + toUserId + "," + quantity + "," + actionType + "," + fromUserId);
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        tx.setRetryCount(0);
        transactionRepository.save(tx);

        RemoteFunctionCall<TransactionReceipt> functionCall =
                blockchainService.getContract().transferItem(
                        BigInteger.valueOf(itemId),
                        BigInteger.valueOf(toUserId),
                        BigInteger.valueOf(quantity),
                        actionType,
                        BigInteger.valueOf(fromUserId));

        // Send blockchain transaction and update database upon completion
        return blockchainService.sendTransactionWithRetry(functionCall, tx)
                .thenApply(txHash -> {
                    // Update the Items table to reflect new ownership
                    try {
                        Optional<Items> itemOpt = itemRepository.findById(itemId);
                        if (itemOpt.isPresent()) {
                            Items item = itemOpt.get();

                            // Find the new owner
                            Optional<Users> newOwnerOpt = userRepository.findById(toUserId);
                            if (newOwnerOpt.isPresent()) {
                                // Update owner reference
                                item.setOwner(newOwnerOpt.get());

                                // Add status history entry
                                item.setStatus("TRANSFERRED");
                                item.setUpdatedAt(new Date());
                                itemRepository.save(item);
                            }
                        }
                    } catch (Exception e) {
                        // Log error but don't fail the transfer
                        System.err.println("Error updating database after blockchain transfer: " + e.getMessage());
                    }
                    return txHash;
                });
    }

    /**
     * Process items to create a new item (manufacturing)
     * @param sourceItemIds List of input item IDs
     * @param newItemId ID for the newly created item
     * @param inputQuantities Quantities of each input item to use
     * @param outputQuantity Quantity of the output item
     * @param newItemType Type of the new item
     * @param processorId User ID of the processor
     * @return CompletableFuture containing the transaction hash
     */
    public CompletableFuture<String> processItem(
            List<Long> sourceItemIds,
            Long newItemId,
            List<Long> inputQuantities,
            Long outputQuantity,
            String newItemType,
            Long processorId) {

        // Convert Lists of Long to Lists of BigInteger
        List<BigInteger> sourceItemIdsBigInt = new ArrayList<>();
        List<BigInteger> inputQuantitiesBigInt = new ArrayList<>();

        for (Long id : sourceItemIds) {
            sourceItemIdsBigInt.add(BigInteger.valueOf(id));
        }

        for (Long qty : inputQuantities) {
            inputQuantitiesBigInt.add(BigInteger.valueOf(qty));
        }

        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setFunction("processItem");
        tx.setParameters(sourceItemIds + "," + newItemId + ","
                + inputQuantities + "," + outputQuantity + "," + newItemType + "," + processorId);
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        tx.setRetryCount(0);
        transactionRepository.save(tx);

        RemoteFunctionCall<TransactionReceipt> functionCall =
                blockchainService.getContract().processItem(
                        sourceItemIdsBigInt,
                        BigInteger.valueOf(newItemId),
                        inputQuantitiesBigInt,
                        BigInteger.valueOf(outputQuantity),
                        newItemType,
                        BigInteger.valueOf(processorId));

        // Send blockchain transaction and update database upon completion
        return blockchainService.sendTransactionWithRetry(functionCall, tx)
                .thenApply(txHash -> {
                    try {
                        // Get necessary data
                        Optional<Users> processor = userRepository.findById(processorId);

                        // Find first source item to get supply chain
                        Optional<Items> firstSourceItem = itemRepository.findById(sourceItemIds.get(0));

                        if (processor.isPresent() && firstSourceItem.isPresent()) {
                            Chains supplyChain = firstSourceItem.get().getSupplyChain();

                            // Create the new processed item in database
                            Items newItem = new Items();
                            newItem.setId(newItemId);
                            newItem.setName(newItemType); // Default name
                            newItem.setItemType(newItemType);
                            newItem.setQuantity(outputQuantity);
                            newItem.setOwner(processor.get());
                            newItem.setSupplyChain(supplyChain);
                            newItem.setStatus("PROCESSING");
                            newItem.setCreatedAt(new Date());
                            newItem.setUpdatedAt(new Date());
                            newItem.setBlockchainTxHash(txHash);
                            newItem.setBlockchainStatus("CONFIRMED");

                            // Store parent IDs
                            newItem.setParentItemIds(sourceItemIds);

                            itemRepository.save(newItem);

                            // Update source items status
                            for (int i = 0; i < sourceItemIds.size(); i++) {
                                Long sourceId = sourceItemIds.get(i);
                                Long usedQuantity = inputQuantities.get(i);

                                Optional<Items> sourceOpt = itemRepository.findById(sourceId);
                                if (sourceOpt.isPresent()) {
                                    Items source = sourceOpt.get();

                                    // Reduce quantity
                                    source.setQuantity(source.getQuantity() - usedQuantity);

                                    // If fully consumed, mark as completed
                                    if (source.getQuantity() <= 0) {
                                        source.setStatus("COMPLETED");
                                    }

                                    source.setUpdatedAt(new Date());
                                    itemRepository.save(source);
                                }
                            }
                        }

                    } catch (Exception e) {
                        // Log error but don't fail the transaction
                        System.err.println("Error updating database after processing: " + e.getMessage());
                    }

                    return txHash;
                });
    }

    /**
     * Updates the status of an item on the blockchain
     * @param itemId ID of the item
     * @param newStatus New status value (0=CREATED, 1=IN_TRANSIT, 2=PROCESSING, 3=COMPLETED, 4=REJECTED)
     * @param ownerId User ID of the owner
     * @return CompletableFuture containing the transaction hash
     */
    public CompletableFuture<String> updateItemStatus(Long itemId, Integer newStatus, Long ownerId) {
        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setFunction("updateItemStatus");
        tx.setParameters(itemId + "," + newStatus + "," + ownerId);
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        tx.setRetryCount(0);
        transactionRepository.save(tx);

        RemoteFunctionCall<TransactionReceipt> functionCall =
                blockchainService.getContract().updateItemStatus(
                        BigInteger.valueOf(itemId),
                        BigInteger.valueOf(newStatus),
                        BigInteger.valueOf(ownerId));

        // Convert numeric status to string for database
        String statusString;
        switch (newStatus) {
            case 0:
                statusString = "CREATED";
                break;
            case 1:
                statusString = "IN_TRANSIT";
                break;
            case 2:
                statusString = "PROCESSING";
                break;
            case 3:
                statusString = "COMPLETED";
                break;
            case 4:
                statusString = "REJECTED";
                break;
            default:
                statusString = "UNKNOWN";
        }

        // Capture status for lambda
        final String finalStatus = statusString;

        return blockchainService.sendTransactionWithRetry(functionCall, tx)
                .thenApply(txHash -> {
                    // Update item status in database
                    try {
                        Optional<Items> itemOpt = itemRepository.findById(itemId);
                        if (itemOpt.isPresent()) {
                            Items item = itemOpt.get();
                            item.setStatus(finalStatus);
                            item.setUpdatedAt(new Date());
                            itemRepository.save(item);
                        }
                    } catch (Exception e) {
                        // Log error but don't fail the status update
                        System.err.println("Error updating database item status: " + e.getMessage());
                    }
                    return txHash;
                });
    }

    public CompletableFuture<String> createOrderOnBlockchain(
            Long orderId,
            Long supplyChainId,
            Long customerId,
            Long quantity
    ) {
        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setFunction("createOrder");
        tx.setParameters(orderId + "," + supplyChainId + "," + customerId);
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        tx.setRetryCount(0);
        transactionRepository.save(tx);

        // Assuming your smart contract has a `createItem()` function that logs orders as blockchain items
        RemoteFunctionCall<TransactionReceipt> functionCall =
                blockchainService.getContract().createItem(
                        BigInteger.valueOf(orderId),
                        BigInteger.valueOf(supplyChainId),
                        BigInteger.valueOf(quantity),
                        "ORDER",
                        BigInteger.valueOf(customerId)
                );

        // Send the transaction and handle DB update
        return blockchainService.sendTransactionWithRetry(functionCall, tx)
                .thenApply(txHash -> {
                    // Optional: Log or update something in your DB if needed
                    System.out.println("Order created on blockchain with txHash: " + txHash);
                    return txHash;
                });
    }

    /**
     * Create an item on the blockchain for a specific order item
     *
     * @param orderId The ID of the parent order
     * @param supplyChainId The ID of the supply chain
     * @param customerId The ID of the customer
     * @param productId The ID of the product
     * @param quantity The quantity ordered
     * @return A CompletableFuture containing the blockchain item ID
     */
    public CompletableFuture<Long> createItemOnBlockchain(Long orderId, Long supplyChainId,
                                                          Long customerId, Long productId,
                                                          Long quantity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Generate a unique item ID using a combination of order ID and product ID
                // This ensures each item has a unique ID on the blockchain
                BigInteger itemId = BigInteger.valueOf(orderId * 10000 + productId);

                // Create a transaction record
                BlockchainTransaction tx = new BlockchainTransaction();
                tx.setFunction("createItem");
                tx.setParameters(itemId + "," + supplyChainId + "," + quantity + ",product," + customerId);
                tx.setStatus("PENDING");
                tx.setCreatedAt(Instant.now());
                tx.setRetryCount(0);
                transactionRepository.save(tx);

                // Convert parameters to BigInteger for the smart contract
                BigInteger chainId = BigInteger.valueOf(supplyChainId);
                BigInteger qty = BigInteger.valueOf(quantity);
                BigInteger creatorId = BigInteger.valueOf(customerId);

                // Set the item type for this product
                String itemType = "product";

                // Call the smart contract's createItem function
                // Based on the contract: createItem(uint256 itemId, uint256 supplyChainId, uint256 quantity, string calldata itemType, uint256 creatorId)
                RemoteFunctionCall<TransactionReceipt> functionCall =
                        blockchainService.getContract().createItem(
                                itemId,          // itemId
                                chainId,         // supplyChainId
                                qty,             // quantity
                                itemType,        // itemType
                                creatorId        // creatorId
                        );

                // Send the transaction
                TransactionReceipt receipt = functionCall.send();
                String txHash = receipt.getTransactionHash();

                // Update transaction record
                tx.setTransactionHash(txHash);
                tx.setStatus("CONFIRMED");
                tx.setConfirmedAt(Instant.now());
                transactionRepository.save(tx);

                // Log the successful transaction
                System.out.println("Created blockchain item with ID: " + itemId +
                        " for order: " + orderId +
                        ", product: " + productId +
                        ", tx hash: " + txHash);

                // Return the blockchain item ID
                return itemId.longValue();
            } catch (Exception e) {
                System.err.println("Error creating item on blockchain: " + e.getMessage());
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Cancel an order on the blockchain by updating the status of associated items
     *
     * @param orderId The ID of the order being cancelled
     * @param supplyChainId The ID of the supply chain
     * @param customerId The ID of the customer cancelling the order
     * @return A CompletableFuture containing the transaction hash
     */
    public CompletableFuture<String> cancelOrderOnBlockchain(Long orderId, Long supplyChainId, Long customerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get all order items with blockchain IDs for this order from the repository
                List<OrderItem> orderItems = orderItemRepository.findByOrder_Id(orderId);

                // Status code for REJECTED/CANCELLED in the contract enum
                Integer rejectedStatus = 4; // ItemStatus.REJECTED = 4

                String lastTxHash = null;

                // Update each item's status on the blockchain
                for (OrderItem item : orderItems) {
                    if (item.getBlockchainItemId() != null) {
                        try {
                            // Create transaction record
                            BlockchainTransaction tx = new BlockchainTransaction();
                            tx.setFunction("updateItemStatus");
                            tx.setParameters(item.getBlockchainItemId() + "," + rejectedStatus + "," + customerId);
                            tx.setStatus("PENDING");
                            tx.setCreatedAt(Instant.now());
                            tx.setRetryCount(0);
                            transactionRepository.save(tx);

                            // Call the updateItemStatus method on the blockchain
                            RemoteFunctionCall<TransactionReceipt> functionCall =
                                    blockchainService.getContract().updateItemStatus(
                                            BigInteger.valueOf(item.getBlockchainItemId()),  // itemId
                                            BigInteger.valueOf(rejectedStatus),              // newStatus
                                            BigInteger.valueOf(customerId)                   // ownerId
                                    );

                            // Send the transaction
                            TransactionReceipt receipt = functionCall.send();
                            lastTxHash = receipt.getTransactionHash();

                            // Update transaction record
                            tx.setTransactionHash(lastTxHash);
                            tx.setStatus("CONFIRMED");
                            tx.setConfirmedAt(Instant.now());
                            transactionRepository.save(tx);

                            System.out.println("Cancelled blockchain item " + item.getBlockchainItemId() +
                                    " for order " + orderId +
                                    ", tx hash: " + lastTxHash);

                            // Update the Items record in the database
                            try {
                                Items blockchainItem = itemRepository.findById(item.getBlockchainItemId())
                                        .orElse(null);

                                if (blockchainItem != null) {
                                    blockchainItem.setStatus("REJECTED");
                                    blockchainItem.setBlockchainStatus("REJECTED");
                                    blockchainItem.setBlockchainTxHash(lastTxHash);
                                    blockchainItem.setUpdatedAt(new Date());
                                    itemRepository.save(blockchainItem);
                                }
                            } catch (Exception e) {
                                System.err.println("Error updating blockchain item in database: " + e.getMessage());
                            }
                        } catch (Exception e) {
                            System.err.println("Error cancelling item " + item.getBlockchainItemId() + ": " + e.getMessage());
                            // Continue with other items even if one fails
                        }
                    }
                }

                // Return the last transaction hash (or null if no items were updated)
                return lastTxHash;
            } catch (Exception e) {
                System.err.println("Error cancelling order on blockchain: " + e.getMessage());
                throw new CompletionException(e);
            }
        });
    }
}