package com.manublock.backend.config;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

@Configuration
public class Web3jConfig {
    @Value("${web3j.client-address}")
    private String rpcUrl;

    @Value("${blockchain.wallet.private-key}")
    private String adminPrivateKey;

    // Expected chain ID for Sepolia
    private static final long EXPECTED_CHAIN_ID = 11155111L;

    // Default gas values - avoiding dynamic calls that create circular dependencies
    private static final BigInteger DEFAULT_GAS_PRICE = BigInteger.valueOf(50_000_000_000L); // 50 Gwei
    private static final BigInteger DEFAULT_GAS_LIMIT = BigInteger.valueOf(700_000);

    @Bean
    public Web3j web3j() {
        // Create HTTP client with longer timeouts for better reliability
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        return Web3j.build(new HttpService(rpcUrl, client));
    }

    @Bean
    public Credentials credentials() {
        return Credentials.create(adminPrivateKey);
    }

    @Bean
    public ContractGasProvider gasProvider() {
        // Using StaticGasProvider to avoid circular dependencies
        // The dynamic gas price will be handled in the service layer
        return new StaticGasProvider(DEFAULT_GAS_PRICE, DEFAULT_GAS_LIMIT);
    }

    @Bean
    public TransactionManager web3jTransactionManager(Web3j web3j, Credentials credentials) {
        TransactionReceiptProcessor receiptProcessor =
                new PollingTransactionReceiptProcessor(web3j, 1000, 60);

        // Use the explicit chain ID for Sepolia
        return new FastRawTransactionManager(web3j, credentials, EXPECTED_CHAIN_ID, receiptProcessor);
    }
}