package com.manublock.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.crypto.Credentials;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import java.math.BigInteger;

@Configuration
public class Web3jConfig {

    @Value("${web3j.client-address}")
    private String rpcUrl;

    @Value("${blockchain.wallet.private-key}")
    private String adminPrivateKey;

    private static final BigInteger GAS_PRICE = BigInteger.valueOf(30_000_000_000L);
    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(4_300_000L);

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(rpcUrl));
    }

    @Bean
    public Credentials credentials() {
        return Credentials.create(adminPrivateKey);
    }

    @Bean
    public ContractGasProvider gasProvider() {
        return new StaticGasProvider(GAS_PRICE, GAS_LIMIT);
    }
}