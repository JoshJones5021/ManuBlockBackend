package com.manublock.backend.services;

import com.manublock.backend.contracts.contract.SmartContract;
import com.manublock.backend.models.BlockchainTransaction;
import com.manublock.backend.repositories.BlockchainTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.protocol.core.RemoteFunctionCall;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@Service
public class BlockchainService {

    private final Web3j web3j;
    private final BlockchainTransactionRepository transactionRepository;
    private final SmartContract contract;

    @Autowired
    public BlockchainService(
            Web3j web3j,
            Credentials credentials,
            ContractGasProvider configGasProvider, // keep for backward compatibility
            DynamicGasProviderService gasProviderService,
            TransactionManager web3jTransactionManager,
            @Value("${blockchain.contract.address}") String contractAddress,
            BlockchainTransactionRepository transactionRepository) {

        this.web3j = web3j;
        this.transactionRepository = transactionRepository;

        // Use dynamic gas provider for loading contracts
        ContractGasProvider dynamicGasProvider = gasProviderService.createDynamicGasProvider();
        this.contract = SmartContract.load(contractAddress, web3j, web3jTransactionManager, dynamicGasProvider);
    }

    private static final Logger logger = Logger.getLogger(BlockchainService.class.getName());

    public SmartContract getContract() {
        return contract;
    }

    /**
     * Helper method to execute blockchain operations with retry logic
     */
    private <T> T executeWithRetry(Callable<T> operation) throws Exception {
        int maxRetries = 5;
        int retryCount = 0;
        long waitTime = 1000; // Start with 1 second

        while (true) {
            try {
                return operation.call();
            } catch (Exception e) {
                // Check if it's a rate limit error (429)
                if ((e.getMessage().contains("429") || e.getMessage().contains("Too Many Requests"))
                        && retryCount < maxRetries) {
                    retryCount++;
                    logger.warning("Rate limit hit. Retrying in " + waitTime + "ms. Attempt " + retryCount + " of " + maxRetries);
                    Thread.sleep(waitTime);
                    waitTime *= 2; // Exponential backoff
                } else {
                    throw e; // Re-throw if not a rate limit error or max retries reached
                }
            }
        }
    }

    /**
     * Executes a smart contract function with retry logic for rate limits
     */
    public <T> T executeContractFunction(Callable<T> contractFunction) throws Exception {
        return executeWithRetry(contractFunction);
    }

    /**
     * Creates a supply chain on blockchain with the admin's wallet
     * Updated to include creator's user ID
     */
    synchronized public CompletableFuture<String> createSupplyChain(Long supplyChainId, Long creatorUserId) {
        System.out.println("📋 Starting createSupplyChain for ID: " + supplyChainId + " by user: " + creatorUserId);

        // Format parameters for the transaction record
        String parameters = supplyChainId + "," + creatorUserId;

        Optional<BlockchainTransaction> existingTx = transactionRepository
                .findByStatus("PENDING")
                .stream()
                .filter(tx -> tx.getFunction().equals("createSupplyChain") &&
                        tx.getParameters().equals(parameters))
                .findFirst();

        if (existingTx.isPresent()) {
            BlockchainTransaction tx = existingTx.get();
            String existingTxHash = tx.getTransactionHash();

            // If transaction exists and has a confirmed hash, return it
            if (existingTxHash != null && !existingTxHash.isEmpty() && "CONFIRMED".equals(tx.getStatus())) {
                System.out.println("✅ Transaction already confirmed on-chain. Hash: " + existingTxHash);
                return CompletableFuture.completedFuture(existingTxHash);
            }
            // If there's a hash but status isn't confirmed, check on-chain
            else if (existingTxHash != null && !existingTxHash.isEmpty()) {
                System.out.println("🔍 Pending transaction found with hash. Checking status on-chain: " + existingTxHash);
                try {
                    Optional<TransactionReceipt> receipt = web3j
                            .ethGetTransactionReceipt(existingTxHash)
                            .send()
                            .getTransactionReceipt();

                    if (receipt.isPresent()) {
                        // Transaction exists on chain, update status and return
                        tx.setStatus("CONFIRMED");
                        tx.setConfirmedAt(Instant.now());
                        transactionRepository.save(tx);
                        System.out.println("✅ Found transaction on-chain. Updated status to CONFIRMED.");
                        return CompletableFuture.completedFuture(existingTxHash);
                    } else {
                        // If no receipt found but 30+ minutes have passed, restart
                        if (tx.getCreatedAt().plusSeconds(1800).isBefore(Instant.now())) {
                            System.out.println("⚠️ Transaction pending for >30 min without receipt. Marking as FAILED and retrying.");
                            tx.setStatus("FAILED");
                            tx.setFailureReason("Timeout waiting for receipt");
                            transactionRepository.save(tx);
                        } else {
                            // Still within reasonable wait time, return the pending hash
                            System.out.println("⏳ Transaction is still pending. Hash: " + existingTxHash);
                            return CompletableFuture.completedFuture(existingTxHash);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Error checking transaction status: " + e.getMessage());
                    // Continue with new transaction if there was an error checking status
                }
            } else {
                System.out.println("⚠️ Old transaction is stuck with no hash. Marking as FAILED and retrying.");
                tx.setStatus("FAILED");
                tx.setFailureReason("No transaction hash received");
                transactionRepository.save(tx);
            }
        }

        // Create a new transaction record
        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setFunction("createSupplyChain");
        tx.setParameters(parameters); // Use the combined parameters string
        tx.setStatus("PENDING");
        tx.setCreatedAt(Instant.now());
        tx.setRetryCount(0);
        transactionRepository.save(tx);
        System.out.println("📝 Created new transaction record in DB");

        // Updated function call with both parameters
        RemoteFunctionCall<TransactionReceipt> functionCall = contract.createSupplyChain(
                BigInteger.valueOf(supplyChainId),
                BigInteger.valueOf(creatorUserId));

        return sendTransactionWithRetry(functionCall, 0, tx)
                .thenApply(txHash -> {
                    if (txHash != null && !txHash.isEmpty()) {
                        System.out.println("✅ Transaction successful! Updating DB with hash: " + txHash);
                        tx.setTransactionHash(txHash);
                        tx.setStatus("CONFIRMED");
                        tx.setConfirmedAt(Instant.now());
                    } else {
                        System.out.println("⚠️ Transaction completed but hash is missing. Marking as FAILED.");
                        tx.setStatus("FAILED");
                        tx.setFailureReason("Missing transaction hash");
                    }

                    transactionRepository.save(tx);
                    return txHash;
                })
                .exceptionally(ex -> {
                    System.out.println("❌ Transaction failed: " + ex.getMessage());
                    tx.setStatus("FAILED");
                    tx.setFailureReason(ex.getMessage());
                    transactionRepository.save(tx);
                    throw new RuntimeException("Transaction failed: " + ex.getMessage(), ex);
                });
    }

    /**
     * Backward compatibility method that defaults to using the admin user ID
     */
    synchronized public CompletableFuture<String> createSupplyChain(Long supplyChainId) {
        // Default to admin user ID (typically ID 1, but use whatever your admin's ID is)
        Long adminUserId = 1L; // Replace with your admin's user ID
        return createSupplyChain(supplyChainId, adminUserId);
    }

    public CompletableFuture<String> sendTransactionWithRetry(
            RemoteFunctionCall<TransactionReceipt> functionCall,
            BlockchainTransaction tx) {

        return sendTransactionWithRetry(functionCall, 0, tx)
                .thenApply(txHash -> {
                    if (txHash != null && !txHash.isEmpty()) {
                        System.out.println("✅ Transaction successful! Updating DB with hash: " + txHash);
                        tx.setTransactionHash(txHash);
                        tx.setStatus("CONFIRMED");
                        tx.setConfirmedAt(Instant.now());
                    } else {
                        System.out.println("⚠️ Transaction completed but hash is missing. Marking as FAILED.");
                        tx.setStatus("FAILED");
                        tx.setFailureReason("Missing transaction hash");
                    }

                    transactionRepository.save(tx);
                    return txHash;
                })
                .exceptionally(ex -> {
                    System.out.println("❌ Transaction failed: " + ex.getMessage());
                    tx.setStatus("FAILED");
                    tx.setFailureReason(ex.getMessage());
                    transactionRepository.save(tx);
                    throw new RuntimeException("Transaction failed: " + ex.getMessage(), ex);
                });
    }

    private CompletableFuture<String> sendTransactionWithRetry(RemoteFunctionCall<TransactionReceipt> functionCall, int retryCount, BlockchainTransaction tx) {
        if (retryCount > 3) {
            System.out.println("❌ Maximum retry attempts reached (" + retryCount + ") for transaction");
            return CompletableFuture.failedFuture(new RuntimeException("Transaction failed after " + retryCount + " retry attempts"));
        }

        // Check if transaction is already confirmed to avoid duplicates
        if (tx.getTransactionHash() != null && !tx.getTransactionHash().isEmpty() && "CONFIRMED".equals(tx.getStatus())) {
            System.out.println("✅ Transaction is already confirmed with hash: " + tx.getTransactionHash());
            return CompletableFuture.completedFuture(tx.getTransactionHash());
        }

        System.out.println("📤 Sending blockchain transaction (attempt #" + (retryCount + 1) + ")");

        return functionCall.sendAsync().handle((receipt, ex) -> {
            if (ex == null && receipt != null) {
                String txHash = receipt.getTransactionHash();
                System.out.println("✅ Transaction successful! Hash: " + txHash);

                // Save transaction hash immediately
                tx.setTransactionHash(txHash);
                tx.setStatus("CONFIRMED");
                tx.setConfirmedAt(Instant.now());
                tx.setRetryCount(retryCount);
                transactionRepository.save(tx);

                return txHash;
            } else {
                System.out.println("⚠️ Transaction attempt failed: " + (ex != null ? ex.getMessage() : "No receipt returned"));

                // Only check blockchain if we already have a transaction hash
                if (tx.getTransactionHash() != null && !tx.getTransactionHash().isEmpty()) {
                    System.out.println("🔍 Checking if transaction exists on blockchain: " + tx.getTransactionHash());

                    try {
                        Optional<TransactionReceipt> confirmedReceipt = web3j
                                .ethGetTransactionReceipt(tx.getTransactionHash())
                                .send()
                                .getTransactionReceipt();

                        if (confirmedReceipt.isPresent()) {
                            String confirmedHash = confirmedReceipt.get().getTransactionHash();
                            System.out.println("✅ Found transaction on-chain! Hash: " + confirmedHash);

                            // Mark as confirmed to prevent retries
                            tx.setTransactionHash(confirmedHash);
                            tx.setStatus("CONFIRMED");
                            tx.setConfirmedAt(Instant.now());
                            tx.setRetryCount(retryCount);
                            transactionRepository.save(tx);

                            return confirmedHash;
                        } else {
                            System.out.println("❓ Transaction not found on blockchain");
                        }
                    } catch (Exception e) {
                        System.out.println("⚠️ Error verifying transaction on-chain: " + e.getMessage());
                    }
                } else {
                    System.out.println("ℹ️ No transaction hash yet to verify on blockchain");
                }

                // Record the attempt before retry
                tx.setStatus("PENDING");
                tx.setLastAttempt(Instant.now()); // Add this field to your entity
                tx.setRetryCount(retryCount + 1);
                transactionRepository.save(tx);

                // Add a small delay before retrying to avoid overwhelming the node
                try {
                    Thread.sleep(2000 * (retryCount + 1)); // Progressive backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                // Retry with incremented retry count
                System.out.println("🔄 Retrying transaction...");
                return sendTransactionWithRetry(functionCall, retryCount + 1, tx).join();
            }
        });
    }
}