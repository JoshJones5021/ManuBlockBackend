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
     */
    public CompletableFuture<String> authorizeParticipant(Long supplyChainId, Long participantUserId) {
        // Find the user's wallet address
        Users user = userRepository.findById(participantUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String walletAddress = user.getWalletAddress();
        if (walletAddress == null || walletAddress.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new RuntimeException("User does not have a connected wallet address"));
        }

        // Create a transaction record
        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setFunction("authorizeParticipant");
        tx.setParameters(supplyChainId + "," + walletAddress);
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        tx.setRetryCount(0);
        transactionRepository.save(tx);

        // Use the admin contract instance
        RemoteFunctionCall<TransactionReceipt> functionCall =
                adminContract.authorizeParticipant(
                        BigInteger.valueOf(supplyChainId),
                        walletAddress);

        // Use the blockchain service's transaction handling mechanism
        return blockchainService.sendTransactionWithRetry(functionCall, tx);
    }

    /**
     * Creates a new supply chain item on the blockchain using admin credentials
     */
    public CompletableFuture<String> createItem(
            Long itemId,
            Long supplyChainId,
            Long quantity,
            String itemType,
            Long ownerId) {

        // Find owner's wallet address for the transaction
        Users owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Owner user not found"));

        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setFunction("createItem");
        tx.setParameters(itemId + "," + supplyChainId + "," + quantity + "," + itemType);
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        tx.setRetryCount(0);
        transactionRepository.save(tx);

        RemoteFunctionCall<TransactionReceipt> functionCall =
                adminContract.createItem(
                        BigInteger.valueOf(itemId),
                        BigInteger.valueOf(supplyChainId),
                        BigInteger.valueOf(quantity),
                        itemType);

        return blockchainService.sendTransactionWithRetry(functionCall, tx);
    }

    /**
     * Transfers an item from one participant to another using admin credentials
     */
    public CompletableFuture<String> transferItem(
            Long itemId,
            String toAddress,
            Long quantity,
            String actionType,
            Long fromUserId) {

        // Find the from user's wallet address
        Users fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new RuntimeException("From user not found"));

        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setFunction("transferItem");
        tx.setParameters(itemId + "," + toAddress + "," + quantity + "," + actionType);
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        tx.setRetryCount(0);
        transactionRepository.save(tx);

        RemoteFunctionCall<TransactionReceipt> functionCall =
                adminContract.transferItem(
                        BigInteger.valueOf(itemId),
                        toAddress,
                        BigInteger.valueOf(quantity),
                        actionType);

        return blockchainService.sendTransactionWithRetry(functionCall, tx);
    }

    /**
     * Process items to create a new item (manufacturing) using admin credentials
     */
    public CompletableFuture<String> processItem(
            List<Long> sourceItemIds,
            Long newItemId,
            List<Long> inputQuantities,
            Long outputQuantity,
            String newItemType,
            Long manufacturerId) {

        // Find manufacturer's wallet address
        Users manufacturer = userRepository.findById(manufacturerId)
                .orElseThrow(() -> new RuntimeException("Manufacturer not found"));

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
                + inputQuantities + "," + outputQuantity + "," + newItemType);
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        tx.setRetryCount(0);
        transactionRepository.save(tx);

        RemoteFunctionCall<TransactionReceipt> functionCall =
                adminContract.processItem(
                        sourceItemIdsBigInt,
                        BigInteger.valueOf(newItemId),
                        inputQuantitiesBigInt,
                        BigInteger.valueOf(outputQuantity),
                        newItemType);

        return blockchainService.sendTransactionWithRetry(functionCall, tx);
    }

    /**
     * Updates the status of an item on the blockchain using admin credentials
     */
    public CompletableFuture<String> updateItemStatus(Long itemId, Integer newStatus, Long ownerId) {
        // Find the owner's wallet address
        Users owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setFunction("updateItemStatus");
        tx.setParameters(itemId + "," + newStatus);
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        tx.setRetryCount(0);
        transactionRepository.save(tx);

        RemoteFunctionCall<TransactionReceipt> functionCall =
                adminContract.updateItemStatus(
                        BigInteger.valueOf(itemId),
                        BigInteger.valueOf(newStatus));

        return blockchainService.sendTransactionWithRetry(functionCall, tx);
    }
}