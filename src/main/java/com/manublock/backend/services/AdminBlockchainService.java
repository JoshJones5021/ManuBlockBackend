package com.manublock.backend.services;

import com.manublock.backend.contracts.contract.SmartContract;
import com.manublock.backend.models.BlockchainTransaction;
import com.manublock.backend.models.Users;
import com.manublock.backend.repositories.BlockchainTransactionRepository;
import com.manublock.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced blockchain service that uses admin credentials for all operations
 * regardless of which user is performing the action
 */
@Service
public class AdminBlockchainService {

    private final BlockchainService blockchainService;
    private final BlockchainTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final Web3j web3j;
    private final Credentials adminCredentials;
    private final TransactionManager adminTransactionManager;
    private final ContractGasProvider gasProvider;
    private final SmartContract adminContract;

    @Autowired
    public AdminBlockchainService(
            BlockchainService blockchainService,
            BlockchainTransactionRepository transactionRepository,
            UserRepository userRepository,
            Web3j web3j,
            Credentials credentials,
            TransactionManager transactionManager,
            ContractGasProvider gasProvider,
            @Value("${blockchain.contract.address}") String contractAddress) {

        this.blockchainService = blockchainService;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.web3j = web3j;
        this.adminCredentials = credentials;
        this.adminTransactionManager = transactionManager;
        this.gasProvider = gasProvider;
        this.adminContract = SmartContract.load(contractAddress, web3j, transactionManager, gasProvider);
    }

    /**
     * Authorizes a participant to interact with a supply chain using admin credentials
     * Now uses user ID instead of wallet address
     */
    public CompletableFuture<String> authorizeParticipant(Long supplyChainId, Long participantUserId) {
        // Find the user to make sure they exist
        Users user = userRepository.findById(participantUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // No longer need to check for wallet address

        // Create a transaction record
        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setFunction("authorizeParticipant");
        tx.setParameters(supplyChainId + "," + participantUserId);
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        tx.setRetryCount(0);
        transactionRepository.save(tx);

        // Use the admin contract instance with user ID (not wallet address)
        RemoteFunctionCall<TransactionReceipt> functionCall =
                adminContract.authorizeParticipant(
                        BigInteger.valueOf(supplyChainId),
                        BigInteger.valueOf(participantUserId)); // Changed from wallet address to user ID

        // Use the blockchain service's transaction handling mechanism
        return blockchainService.sendTransactionWithRetry(functionCall, tx);
    }

    /**
     * Creates a new supply chain item on the blockchain using admin credentials
     * Now uses creator user ID instead of wallet address
     */
    public CompletableFuture<String> createItem(
            Long itemId,
            Long supplyChainId,
            Long quantity,
            String itemType,
            Long creatorId) {

        // Find owner to make sure they exist
        Users creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Creator user not found"));

        // Create a transaction record
        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setFunction("createItem");
        tx.setParameters(itemId + "," + supplyChainId + "," + quantity + "," + itemType + "," + creatorId);
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        tx.setRetryCount(0);
        transactionRepository.save(tx);

        // Updated function call with creator user ID
        RemoteFunctionCall<TransactionReceipt> functionCall =
                adminContract.createItem(
                        BigInteger.valueOf(itemId),
                        BigInteger.valueOf(supplyChainId),
                        BigInteger.valueOf(quantity),
                        itemType,
                        BigInteger.valueOf(creatorId)); // Added creator ID parameter

        return blockchainService.sendTransactionWithRetry(functionCall, tx);
    }

    /**
     * Transfers an item from one participant to another using admin credentials
     * Now uses user IDs instead of wallet addresses
     */
    public CompletableFuture<String> transferItem(
            Long itemId,
            Long toUserId,         // Changed from String toAddress
            Long quantity,
            String actionType,
            Long fromUserId) {

        // Verify both users exist
        Users fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new RuntimeException("From user not found"));

        Users toUser = userRepository.findById(toUserId)
                .orElseThrow(() -> new RuntimeException("To user not found"));

        // Create a transaction record
        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setFunction("transferItem");
        tx.setParameters(itemId + "," + toUserId + "," + quantity + "," + actionType + "," + fromUserId);
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        tx.setRetryCount(0);
        transactionRepository.save(tx);

        // Updated function call with user IDs instead of wallet addresses
        RemoteFunctionCall<TransactionReceipt> functionCall =
                adminContract.transferItem(
                        BigInteger.valueOf(itemId),
                        BigInteger.valueOf(toUserId),    // Changed to user ID
                        BigInteger.valueOf(quantity),
                        actionType,
                        BigInteger.valueOf(fromUserId)); // Added from user ID parameter

        return blockchainService.sendTransactionWithRetry(functionCall, tx);
    }

    /**
     * Process items to create a new item (manufacturing) using admin credentials
     * Now includes processor user ID as a parameter
     */
    public CompletableFuture<String> processItem(
            List<Long> sourceItemIds,
            Long newItemId,
            List<Long> inputQuantities,
            Long outputQuantity,
            String newItemType,
            Long processorId) {      // Renamed from manufacturerId to processorId for clarity

        // Verify processor exists
        Users processor = userRepository.findById(processorId)
                .orElseThrow(() -> new RuntimeException("Processor not found"));

        // Convert Lists of Long to Lists of BigInteger
        List<BigInteger> sourceItemIdsBigInt = new ArrayList<>();
        List<BigInteger> inputQuantitiesBigInt = new ArrayList<>();

        for (Long id : sourceItemIds) {
            sourceItemIdsBigInt.add(BigInteger.valueOf(id));
        }

        for (Long qty : inputQuantities) {
            inputQuantitiesBigInt.add(BigInteger.valueOf(qty));
        }

        // Create a transaction record
        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setFunction("processItem");
        tx.setParameters(sourceItemIds + "," + newItemId + ","
                + inputQuantities + "," + outputQuantity + "," + newItemType + "," + processorId);
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        tx.setRetryCount(0);
        transactionRepository.save(tx);

        // Updated function call with processor ID parameter
        RemoteFunctionCall<TransactionReceipt> functionCall =
                adminContract.processItem(
                        sourceItemIdsBigInt,
                        BigInteger.valueOf(newItemId),
                        inputQuantitiesBigInt,
                        BigInteger.valueOf(outputQuantity),
                        newItemType,
                        BigInteger.valueOf(processorId)); // Added processor ID parameter

        return blockchainService.sendTransactionWithRetry(functionCall, tx);
    }

    /**
     * Updates the status of an item on the blockchain using admin credentials
     * Now includes owner user ID as a parameter
     */
    public CompletableFuture<String> updateItemStatus(Long itemId, Integer newStatus, Long ownerId) {
        // Verify owner exists
        Users owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        // Create a transaction record
        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setFunction("updateItemStatus");
        tx.setParameters(itemId + "," + newStatus + "," + ownerId);
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        tx.setRetryCount(0);
        transactionRepository.save(tx);

        // Updated function call with owner ID parameter
        RemoteFunctionCall<TransactionReceipt> functionCall =
                adminContract.updateItemStatus(
                        BigInteger.valueOf(itemId),
                        BigInteger.valueOf(newStatus),
                        BigInteger.valueOf(ownerId)); // Added owner ID parameter

        return blockchainService.sendTransactionWithRetry(functionCall, tx);
    }
}