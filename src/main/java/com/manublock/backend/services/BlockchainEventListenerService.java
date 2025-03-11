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
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service to listen to smart contract events and update local database
 * with rate limiting to avoid Infura 429 errors
 */
@Service
public class BlockchainEventListenerService {

    private final Web3j web3j;
    private final BlockchainService blockchainService;
    private final ChainService chainService;
    private final BlockchainTransactionRepository blockchainTransactionRepository;

    // Rate limiting settings
    @Value("${blockchain.polling.interval:10}")
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
        System.out.println("Setting up blockchain event polling with " + pollingIntervalSeconds + " second interval");

        // Use a scheduled executor instead of continuous polling
        scheduledExecutor = Executors.newScheduledThreadPool(1);
        scheduledExecutor.scheduleWithFixedDelay(
                this::pollBlockchainEvents,
                10, // Initial delay
                pollingIntervalSeconds, // Polling interval
                TimeUnit.SECONDS
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

            // Create filter for latest block only
            EthFilter filter = new EthFilter(
                    DefaultBlockParameterName.LATEST,
                    DefaultBlockParameterName.LATEST,
                    contractAddress);

            // Check for recent transactions and update database
            checkPendingTransactions();

            // Log polling activity (can be removed in production)
            System.out.println("✅ Polled blockchain events successfully");
        } catch (Exception e) {
            System.err.println("❌ Error polling blockchain events: " + e.getMessage());

            // If it's a rate limit error, increase the backoff
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                System.err.println("Rate limit hit, consider increasing polling interval in application.properties");
            }
        }
    }

    /**
     * Check for pending transactions and update their status
     */
    private void checkPendingTransactions() {
        try {
            List<BlockchainTransaction> pendingTxs = blockchainTransactionRepository.findByStatus("PENDING");

            for (BlockchainTransaction tx : pendingTxs) {
                if (tx.getTransactionHash() != null && !tx.getTransactionHash().isEmpty()) {
                    // Check if transaction has been confirmed
                    web3j.ethGetTransactionReceipt(tx.getTransactionHash())
                            .sendAsync()
                            .thenAccept(receipt -> {
                                if (receipt.getTransactionReceipt().isPresent()) {
                                    // Transaction confirmed
                                    tx.setStatus("CONFIRMED");
                                    blockchainTransactionRepository.save(tx);

                                    // If it's a supply chain creation, update the chain status
                                    if ("createSupplyChain".equals(tx.getFunction())) {
                                        try {
                                            Long chainId = Long.valueOf(tx.getParameters());
                                            chainService.updateBlockchainInfo(chainId, tx.getTransactionHash());
                                        } catch (Exception e) {
                                            System.err.println("Error updating supply chain status: " + e.getMessage());
                                        }
                                    }
                                }
                            })
                            .exceptionally(e -> {
                                System.err.println("Error checking transaction receipt: " + e.getMessage());
                                return null;
                            });
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking pending transactions: " + e.getMessage());
        }
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("Cleaning up blockchain event subscriptions...");

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