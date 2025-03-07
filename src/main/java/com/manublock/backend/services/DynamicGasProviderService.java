package com.manublock.backend.services;

import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;

/**
 * Service to provide dynamic gas prices for blockchain transactions
 * This avoids circular dependencies in the configuration layer
 */
@Service
public class DynamicGasProviderService {
    private final Web3j web3j;

    // Default gas values as fallback
    private static final BigInteger DEFAULT_GAS_PRICE = BigInteger.valueOf(50_000_000_000L); // 50 Gwei
    private static final BigInteger DEFAULT_GAS_LIMIT = BigInteger.valueOf(700_000);

    public DynamicGasProviderService(Web3j web3j) {
        this.web3j = web3j;
    }

    /**
     * Gets the current gas price from the blockchain and applies a multiplier
     * @return the current gas price with a 1.2x multiplier for better transaction success
     */
    public BigInteger getCurrentGasPrice() {
        try {
            BigInteger currentGasPrice = web3j.ethGasPrice().send().getGasPrice();
            // Multiply by 1.2 for better transaction success
            return currentGasPrice.multiply(BigInteger.valueOf(12)).divide(BigInteger.valueOf(10));
        } catch (Exception e) {
            System.out.println("⚠️ Could not fetch gas price: " + e.getMessage());
            return DEFAULT_GAS_PRICE;
        }
    }

    /**
     * Creates a custom gas provider with the latest gas price
     * @return a contract gas provider with dynamic gas price
     */
    public ContractGasProvider createDynamicGasProvider() {
        return new ContractGasProvider() {
            @Override
            public BigInteger getGasPrice(String contractFunc) {
                return getCurrentGasPrice();
            }

            @Override
            public BigInteger getGasPrice() {
                return getGasPrice(null);
            }

            @Override
            public BigInteger getGasLimit(String contractFunc) {
                return DEFAULT_GAS_LIMIT;
            }

            @Override
            public BigInteger getGasLimit() {
                return getGasLimit(null);
            }
        };
    }
}