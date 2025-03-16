package com.manublock.backend.services;

import com.manublock.backend.models.BlockchainTransaction;
import com.manublock.backend.models.Users;
import com.manublock.backend.repositories.BlockchainTransactionRepository;
import com.manublock.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.RemoteFunctionCall;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ExtendedBlockchainService {

    private final BlockchainService blockchainService;
    private final BlockchainTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Autowired
    public ExtendedBlockchainService(
            BlockchainService blockchainService,
            BlockchainTransactionRepository transactionRepository,
            UserRepository userRepository) {
        this.blockchainService = blockchainService;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
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
     * @param itemId Unique ID for the item
     * @param supplyChainId The supply chain this item belongs to
     * @param quantity Initial quantity
     * @param itemType Type descriptor (raw material, product, etc.)
     * @param creatorId The user ID of the creator
     * @return CompletableFuture containing the transaction hash
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

        return blockchainService.sendTransactionWithRetry(functionCall, tx);
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

        return blockchainService.sendTransactionWithRetry(functionCall, tx);
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

        return blockchainService.sendTransactionWithRetry(functionCall, tx);
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

        return blockchainService.sendTransactionWithRetry(functionCall, tx);
    }
}