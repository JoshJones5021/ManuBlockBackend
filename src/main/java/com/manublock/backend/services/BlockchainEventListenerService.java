package com.manublock.backend.services;

import com.manublock.backend.contracts.contract.SmartContract;
import com.manublock.backend.models.BlockchainTransaction;
import com.manublock.backend.repositories.BlockchainTransactionRepository;
import io.reactivex.disposables.Disposable;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Service to listen to smart contract events and update local database
 */
@Service
public class BlockchainEventListenerService {

    private final Web3j web3j;
    private final BlockchainService blockchainService;
    private final ChainService chainService;
    private final BlockchainTransactionRepository blockchainTransactionRepository;

    private List<Disposable> subscriptions = new ArrayList<>();

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
    public void subscribeToEvents() {
        try {
            // Get the contract instance
            SmartContract contract = blockchainService.getContract();
            String contractAddress = contract.getContractAddress();

            // Create filter for events from our contract address
            EthFilter filter = new EthFilter(
                    DefaultBlockParameterName.LATEST,
                    DefaultBlockParameterName.LATEST,
                    contractAddress);

            // Subscribe to SupplyChainCreated events
            Disposable supplyChainCreatedSubscription = contract.supplyChainCreatedEventFlowable(filter)
                    .subscribe(event -> {
                        System.out.println("🔗 Supply Chain Created Event Received: " + event.supplyChainId);
                        handleSupplyChainCreatedEvent(event);
                    }, error -> {
                        System.err.println("❌ Error in supply chain event subscription: " + error.getMessage());
                    });
            subscriptions.add(supplyChainCreatedSubscription);

            // Subscribe to ParticipantAuthorized events
            Disposable participantAuthorizedSubscription = contract.participantAuthorizedEventFlowable(filter)
                    .subscribe(event -> {
                        System.out.println("👤 Participant Authorized Event Received: Chain " +
                                event.supplyChainId + ", Participant: " + event.participant);
                        handleParticipantAuthorizedEvent(event);
                    }, error -> {
                        System.err.println("❌ Error in participant event subscription: " + error.getMessage());
                    });
            subscriptions.add(participantAuthorizedSubscription);

            // Subscribe to ItemCreated events
            Disposable itemCreatedSubscription = contract.itemCreatedEventFlowable(filter)
                    .subscribe(event -> {
                        System.out.println("📦 Item Created Event Received: " + event.itemId);
                        handleItemCreatedEvent(event);
                    }, error -> {
                        System.err.println("❌ Error in item created event subscription: " + error.getMessage());
                    });
            subscriptions.add(itemCreatedSubscription);

            // Subscribe to ItemTransferred events
            Disposable itemTransferredSubscription = contract.itemTransferredEventFlowable(filter)
                    .subscribe(event -> {
                        System.out.println("🔄 Item Transferred Event: " + event.itemId +
                                " from " + event.from + " to " + event.to);
                        handleItemTransferredEvent(event);
                    }, error -> {
                        System.err.println("❌ Error in item transfer event subscription: " + error.getMessage());
                    });
            subscriptions.add(itemTransferredSubscription);

            System.out.println("✅ Successfully subscribed to blockchain events");
        } catch (Exception e) {
            System.err.println("❌ Failed to subscribe to blockchain events: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleSupplyChainCreatedEvent(SmartContract.SupplyChainCreatedEventResponse event) {
        try {
            Long supplyChainId = event.supplyChainId.longValue();
            String txHash = event.log.getTransactionHash();

            // Update the supply chain with blockchain transaction hash and status
            chainService.updateBlockchainInfo(supplyChainId, txHash);

            // Update any pending transactions
            List<BlockchainTransaction> pendingTxs = blockchainTransactionRepository.findByStatus("PENDING");
            for (BlockchainTransaction tx : pendingTxs) {
                if ("createSupplyChain".equals(tx.getFunction()) && tx.getParameters().equals(supplyChainId.toString())) {
                    tx.setStatus("CONFIRMED");
                    tx.setTransactionHash(txHash);
                    blockchainTransactionRepository.save(tx);
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error handling SupplyChainCreated event: " + e.getMessage());
        }
    }

    private void handleParticipantAuthorizedEvent(SmartContract.ParticipantAuthorizedEventResponse event) {
        try {
            // Here you would update your local database to reflect participant authorization
            // This could involve finding users by wallet address and updating their permissions

            // Also update pending transactions
            String txHash = event.log.getTransactionHash();
            List<BlockchainTransaction> pendingTxs = blockchainTransactionRepository.findByStatus("PENDING");
            for (BlockchainTransaction tx : pendingTxs) {
                if ("authorizeParticipant".equals(tx.getFunction()) &&
                        tx.getParameters().contains(event.supplyChainId.toString())) {
                    tx.setStatus("CONFIRMED");
                    tx.setTransactionHash(txHash);
                    blockchainTransactionRepository.save(tx);
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error handling ParticipantAuthorized event: " + e.getMessage());
        }
    }

    private void handleItemCreatedEvent(SmartContract.ItemCreatedEventResponse event) {
        try {
            // Update pending transactions for item creation
            String txHash = event.log.getTransactionHash();
            List<BlockchainTransaction> pendingTxs = blockchainTransactionRepository.findByStatus("PENDING");
            for (BlockchainTransaction tx : pendingTxs) {
                if ("createItem".equals(tx.getFunction()) &&
                        tx.getParameters().contains(event.itemId.toString())) {
                    tx.setStatus("CONFIRMED");
                    tx.setTransactionHash(txHash);
                    blockchainTransactionRepository.save(tx);
                    break;
                }
            }

            // Here you would update your local database with the item information
            // including creator, quantity, type, and supply chain ID
        } catch (Exception e) {
            System.err.println("❌ Error handling ItemCreated event: " + e.getMessage());
        }
    }

    private void handleItemTransferredEvent(SmartContract.ItemTransferredEventResponse event) {
        try {
            // Update pending transactions for item transfers
            String txHash = event.log.getTransactionHash();
            List<BlockchainTransaction> pendingTxs = blockchainTransactionRepository.findByStatus("PENDING");
            for (BlockchainTransaction tx : pendingTxs) {
                if ("transferItem".equals(tx.getFunction()) &&
                        tx.getParameters().contains(event.itemId.toString())) {
                    tx.setStatus("CONFIRMED");
                    tx.setTransactionHash(txHash);
                    blockchainTransactionRepository.save(tx);
                    break;
                }
            }

            // Here you would update your local database to reflect the transfer
            // This could involve updating ownership records, quantities, etc.
        } catch (Exception e) {
            System.err.println("❌ Error handling ItemTransferred event: " + e.getMessage());
        }
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("Cleaning up blockchain event subscriptions...");
        for (Disposable subscription : subscriptions) {
            if (!subscription.isDisposed()) {
                subscription.dispose();
            }
        }
    }
}