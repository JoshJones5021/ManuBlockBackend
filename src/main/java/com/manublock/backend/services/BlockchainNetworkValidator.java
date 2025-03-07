package com.manublock.backend.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthChainId;

import java.math.BigInteger;
import java.util.Map;

@Component
public class BlockchainNetworkValidator {

    private final Web3j web3j;
    private final String rpcUrl;

    // Expected chain ID for Sepolia
    private static final long EXPECTED_CHAIN_ID = 11155111L;

    private final Map<Long, String> KNOWN_NETWORKS = Map.of(
            1L, "Ethereum Mainnet",
            11155111L, "Sepolia Testnet",
            5L, "Goerli Testnet",
            137L, "Polygon Mainnet",
            80001L, "Polygon Mumbai Testnet"
    );

    public BlockchainNetworkValidator(
            Web3j web3j,
            @Value("${web3j.client-address}") String rpcUrl) {
        this.web3j = web3j;
        this.rpcUrl = rpcUrl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateNetwork() {
        try {
            // Check if web3j is connected
            boolean connected = !web3j.netVersion().send().hasError();
            if (!connected) {
                System.out.println("❌ Failed to connect to blockchain at: " + rpcUrl);
                return;
            }

            // Get the actual chain ID from the connected network
            EthChainId chainIdResponse = web3j.ethChainId().send();
            if (chainIdResponse.hasError()) {
                System.out.println("❌ Error getting chain ID: " + chainIdResponse.getError().getMessage());
                return;
            }

            long actualChainId = chainIdResponse.getChainId().longValue();
            String networkName = KNOWN_NETWORKS.getOrDefault(actualChainId, "Unknown Network");

            System.out.println("🔗 Blockchain Connection Info:");
            System.out.println("   RPC URL: " + rpcUrl);
            System.out.println("   Connected to: " + networkName + " (Chain ID: " + actualChainId + ")");

            // Validate that we're connected to the expected network
            if (actualChainId != EXPECTED_CHAIN_ID) {
                String expectedNetwork = KNOWN_NETWORKS.get(EXPECTED_CHAIN_ID);
                System.out.println("⚠️ WARNING: Connected to wrong network!");
                System.out.println("   Expected: " + expectedNetwork + " (Chain ID: " + EXPECTED_CHAIN_ID + ")");
                System.out.println("   Connected to: " + networkName + " (Chain ID: " + actualChainId + ")");

                // You can either log a warning or throw an exception based on your requirements
                System.out.println("⚠️ Application will continue but blockchain transactions may fail!");
                // throw new RuntimeException("Wrong blockchain network");
            } else {
                System.out.println("✅ Successfully connected to " + networkName);
            }

            // Additional useful information
            try {
                BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
                System.out.println("   Current gas price: " + gasPrice.doubleValue() / 1_000_000_000 + " Gwei");
            } catch (Exception e) {
                System.out.println("   Could not fetch gas price: " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("❌ Blockchain connection validation failed: " + e.getMessage());
        }
    }
}