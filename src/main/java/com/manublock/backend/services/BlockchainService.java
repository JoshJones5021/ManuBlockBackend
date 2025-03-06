package com.manublock.backend.services;

import com.manublock.backend.models.BlockchainTransaction;
import com.manublock.backend.repositories.BlockchainTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class BlockchainService {

    private final Object web3j;
    private final Object credentials;
    private final Object gasProvider;
    private final String contractAddress;
    private final BlockchainTransactionRepository transactionRepository;

    @Autowired
    public BlockchainService(
            @Qualifier("web3j") Object web3j,
            @Qualifier("credentials") Object credentials,
            @Qualifier("gasProvider") Object gasProvider,
            @Value("${blockchain.contract.address}") String contractAddress,
            BlockchainTransactionRepository transactionRepository) {

        this.web3j = web3j;
        this.credentials = credentials;
        this.gasProvider = gasProvider;
        this.contractAddress = contractAddress;
        this.transactionRepository = transactionRepository;
    }

    public CompletableFuture<String> createSupplyChain(Long supplyChainId) {
        try {
            // Record transaction in database
            BlockchainTransaction tx = new BlockchainTransaction();
            tx.setFunction("createSupplyChain");
            tx.setParameters(supplyChainId.toString());
            tx.setStatus("PENDING");
            tx.setCreatedAt(Instant.now());
            transactionRepository.save(tx);

            // Load contract class
            Class<?> contractClass = Class.forName("org.web3j.tx.Contract");
            Class<?> remoteFunctionCallClass = Class.forName("org.web3j.protocol.core.RemoteFunctionCall");

            // Create function parameters
            Object functionCallResult = callContractFunction(
                    "createSupplyChain",
                    Arrays.asList(BigInteger.valueOf(supplyChainId))
            );

            // Get the completable future from the function call
            Method sendAsyncMethod = remoteFunctionCallClass.getMethod("sendAsync");
            CompletableFuture<?> futureReceipt = (CompletableFuture<?>) sendAsyncMethod.invoke(functionCallResult);

            // Process the transaction receipt
            return futureReceipt.thenApply(receipt -> {
                try {
                    // Extract transaction hash
                    Method getTxHashMethod = receipt.getClass().getMethod("getTransactionHash");
                    String txHash = (String) getTxHashMethod.invoke(receipt);

                    // Update database record
                    tx.setTransactionHash(txHash);
                    tx.setStatus("CONFIRMED");
                    tx.setConfirmedAt(Instant.now());
                    transactionRepository.save(tx);

                    return txHash;
                } catch (Exception e) {
                    tx.setStatus("FAILED");
                    transactionRepository.save(tx);
                    throw new RuntimeException("Failed to process transaction receipt", e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to call createSupplyChain", e);
        }
    }

    private Object callContractFunction(String functionName, List<Object> parameters) {
        try {
            // Load contract using Web3j
            Class<?> contractLoadClass = Class.forName("org.web3j.tx.Contract");
            Method loadMethod = contractLoadClass.getMethod("load",
                    String.class,
                    Class.forName("org.web3j.protocol.Web3j"),
                    Class.forName("org.web3j.crypto.Credentials"),
                    Class.forName("org.web3j.tx.gas.ContractGasProvider"));

            // Get the contract binary
            String binary = ""; // You need to get this from your compiled contract

            // Load the contract
            Object contract = loadMethod.invoke(null, contractAddress, web3j, credentials, gasProvider);

            // Find the method to call on the contract
            Method[] methods = contract.getClass().getMethods();
            for (Method method : methods) {
                if (method.getName().equals(functionName)) {
                    return method.invoke(contract, parameters.toArray());
                }
            }
            throw new RuntimeException("Method " + functionName + " not found on contract");
        } catch (Exception e) {
            throw new RuntimeException("Failed to call contract function: " + functionName, e);
        }
    }

    // Add other methods for contract interactions
}