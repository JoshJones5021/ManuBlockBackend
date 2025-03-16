package com.manublock.backend.services;

import com.manublock.backend.contracts.contract.SmartContract;
import com.manublock.backend.models.BlockchainTransaction;
import com.manublock.backend.repositories.BlockchainTransactionRepository;
import io.reactivex.disposables.Disposable;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service to listen to smart contract events and update local database
 * with rate limiting to avoid Infura 429 errors
 */
@Service
public class BlockchainEventListenerService {
    private static final Logger LOGGER = Logger.getLogger(BlockchainEventListenerService.class.getName());
    private static final int MAX_TRANSACTIONS_PER_BATCH = 5;
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int RETRY_EXPIRATION_HOURS = 24;

    private final Web3j web3j;
    private final BlockchainService blockchainService;
    private final ChainService chainService;
    private final BlockchainTransactionRepository blockchainTransactionRepository;

    // Rate limiting settings with a longer polling interval to reduce API calls
    @Value("${blockchain.polling.interval:600}")
    private int pollingIntervalSeconds;

    private List<Disposable> subscriptions = new ArrayList<>();
    private ScheduledExecutorService scheduledExecutor;

    @Autowired
    public BlockchainEventListenerService(
            Web3j web3j,
            BlockchainService blockchainService,
            ChainService chainService,
            BlockchainTransactionRepository blockchainTransactionRepository) {
        this.web3j = web3j;
        this.blockchainService = blockchainService;
        this.chainService = chainService;
        this.blockchainTransactionRepository = blockchainTransactionRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void setupPolling() {
        LOGGER.info("Setting up blockchain event polling with " + pollingIntervalSeconds + " second interval");

        // Use a scheduled executor instead of continuous polling
        scheduledExecutor = Executors.newScheduledThreadPool(1);
        scheduledExecutor.scheduleWithFixedDelay(
                this::pollBlockchainEvents,
                30, // Initial delay (30 seconds to allow system to start up properly)
                pollingIntervalSeconds, // Polling interval (default changed to 10 minutes)
                TimeUnit.SECONDS
        );

        // Schedule a cleanup task to run daily
        scheduledExecutor.scheduleAtFixedRate(
                this::cleanupStaleTransactions,
                1, // Initial delay
                24, // Run once per day
                TimeUnit.HOURS
        );
    }

    /**
     * Method to poll for events manually instead of using continuous subscriptions
     */
    private void pollBlockchainEvents() {
        try {
            // Get the contract instance
            SmartContract contract = blockchainService.getContract();
            String contractAddress = contract.getContractAddress();

            // Check for recent transactions and update database
            checkPendingTransactions();

            LOGGER.info("✅ Polled blockchain events successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error polling blockchain events", e);

            // If it's a rate limit error, log but don't take action - the fixed delay will handle backoff
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                LOGGER.warning("Rate limit hit, waiting for next scheduled poll");
            }
        }
    }

    /**
     * Check for pending transactions and update their status
     * Limited to processing a maximum number of transactions per batch
     */
    private void checkPendingTransactions() {
        try {
            List<BlockchainTransaction> pendingTxs = blockchainTransactionRepository.findByStatus("PENDING");

            // Process only a limited batch size to avoid rate limiting
            int txCount = Math.min(pendingTxs.size(), MAX_TRANSACTIONS_PER_BATCH);
            LOGGER.info("Processing " + txCount + " of " + pendingTxs.size() + " pending transactions");

            for (int i = 0; i < txCount; i++) {
                BlockchainTransaction tx = pendingTxs.get(i);

                // Skip transactions without hash
                if (tx.getTransactionHash() == null || tx.getTransactionHash().isEmpty()) {
                    continue;
                }

                try {
                    // Check if transaction has been confirmed
                    web3j.ethGetTransactionReceipt(tx.getTransactionHash())
                            .sendAsync()
                            .thenAccept(receipt -> {
                                if (receipt.getTransactionReceipt().isPresent()) {
                                    // Transaction confirmed
                                    tx.setStatus("CONFIRMED");
                                    tx.setConfirmedAt(Instant.now());
                                    blockchainTransactionRepository.save(tx);
                                    LOGGER.info("Transaction confirmed: " + tx.getTransactionHash());

                                    // If it's a supply chain creation, update the chain status
                                    if ("createSupplyChain".equals(tx.getFunction())) {
                                        try {
                                            Long chainId = Long.valueOf(tx.getParameters());
                                            chainService.updateBlockchainInfo(chainId, tx.getTransactionHash());
                                        } catch (Exception e) {
                                            LOGGER.log(Level.SEVERE, "Error updating supply chain status", e);
                                        }
                                    }
                                } else {
                                    // Transaction not yet confirmed - update last attempt
                                    tx.setLastAttempt(Instant.now());
                                    blockchainTransactionRepository.save(tx);
                                }
                            })
                            .exceptionally(e -> {
                                LOGGER.log(Level.WARNING, "Error checking transaction receipt", e);
                                return null;
                            });

                    // Add a small delay between transactions to avoid rate limiting
                    Thread.sleep(200);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error processing transaction: " + tx.getTransactionHash(), e);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking pending transactions", e);
        }
    }

    /**
     * Clean up stale transactions that have been retried too many times
     * or have been pending for too long
     */
    private void cleanupStaleTransactions() {
        try {
            LOGGER.info("Running stale transaction cleanup");

            List<BlockchainTransaction> pendingTxs = blockchainTransactionRepository.findByStatus("PENDING");
            Instant expirationTime = Instant.now().minus(RETRY_EXPIRATION_HOURS, TimeUnit.HOURS.toChronoUnit());

            int cleanedUp = 0;
            for (BlockchainTransaction tx : pendingTxs) {
                // Mark as failed if too many retries or too old
                if (tx.getRetryCount() >= MAX_RETRY_ATTEMPTS ||
                        (tx.getCreatedAt() != null && tx.getCreatedAt().isBefore(expirationTime))) {

                    tx.setStatus("FAILED");
                    tx.setFailureReason("Transaction timed out or exceeded retry limit");
                    blockchainTransactionRepository.save(tx);
                    cleanedUp++;

                    LOGGER.info("Marked stale transaction as failed: " + tx.getId() +
                            " (retries: " + tx.getRetryCount() +
                            ", age: " + (tx.getCreatedAt() != null ?
                            java.time.Duration.between(tx.getCreatedAt(), Instant.now()).toHours() + " hours" :
                            "unknown") + ")");
                }
            }

            LOGGER.info("Cleaned up " + cleanedUp + " stale transactions");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error cleaning up stale transactions", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        LOGGER.info("Cleaning up blockchain event subscriptions...");

        // Dispose RxJava subscriptions if any
        for (Disposable subscription : subscriptions) {
            if (!subscription.isDisposed()) {
                subscription.dispose();
            }
        }

        // Shutdown the scheduled executor
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdownNow();
        }
    }
}