package com.manublock.backend.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.protocol.core.methods.request.Transaction;

import com.manublock.backend.models.Nodes;
import com.manublock.backend.repositories.NodeRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/node-authorization")
public class NodeAuthorizationController {

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private Web3j web3j;

    @Value("${blockchain.contract.address}")
    private String contractAddress;

    // Cache to reduce blockchain calls
    private Map<String, Boolean> authorizationCache = new ConcurrentHashMap<>();
    private Map<String, Long> lastCheckTime = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY = 60000; // 1 minute in milliseconds

    @GetMapping("/supply-chain/{supplyChainId}")
    public ResponseEntity<?> checkSupplyChainNodes(@PathVariable Long supplyChainId) {
        try {
            List<Nodes> nodes = nodeRepository.findBySupplyChain_Id(supplyChainId);

            List<Map<String, Object>> results = new ArrayList<>();

            for (Nodes node : nodes) {
                Map<String, Object> nodeResult = new HashMap<>();
                nodeResult.put("id", node.getId());
                nodeResult.put("name", node.getName());

                if (node.getAssignedUser() != null) {
                    nodeResult.put("userId", node.getAssignedUser().getId());
                    nodeResult.put("userName", node.getAssignedUser().getUsername());

                    // Check authorization
                    boolean isAuthorized = false;
                    try {
                        String cacheKey = supplyChainId + ":" + node.getAssignedUser().getId();
                        Long lastCheck = lastCheckTime.get(cacheKey);

                        if (lastCheck != null && System.currentTimeMillis() - lastCheck < CACHE_EXPIRY) {
                            Boolean cachedResult = authorizationCache.get(cacheKey);
                            if (cachedResult != null) {
                                isAuthorized = cachedResult;
                            }
                        } else {
                            isAuthorized = checkUserAuthorization(supplyChainId, node.getAssignedUser().getId());
                            authorizationCache.put(cacheKey, isAuthorized);
                            lastCheckTime.put(cacheKey, System.currentTimeMillis());
                        }
                    } catch (Exception e) {
                        nodeResult.put("error", e.getMessage());
                    }

                    nodeResult.put("authorized", isAuthorized);
                } else {
                    nodeResult.put("authorized", false);
                }

                results.add(nodeResult);
            }

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to check authorization status: " + e.getMessage()
            ));
        }
    }

    /**
     * Check if user is authorized for a supply chain directly on the blockchain
     */
    private boolean checkUserAuthorization(Long supplyChainId, Long userId) throws Exception {
        // Create function call to check if user is authorized
        Function function = new Function(
                "isParticipantAuthorized",
                Arrays.asList(
                        new org.web3j.abi.datatypes.generated.Uint256(supplyChainId),
                        new org.web3j.abi.datatypes.generated.Uint256(userId)
                ),
                Arrays.asList(new TypeReference<Bool>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        // Make the call to the blockchain
        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(
                        "0x0000000000000000000000000000000000000000",
                        contractAddress, // contract address
                        encodedFunction // encoded function call
                ),
                DefaultBlockParameterName.LATEST
        ).send();

        // Handle error response
        if (response.hasError()) {
            throw new Exception("Error calling blockchain: " + response.getError().getMessage());
        }

        // Parse the response
        String value = response.getValue();

        // If empty or "0x", user is not authorized
        if (value == null || value.equals("0x") || value.equals("0x0")) {
            return false;
        }

        // The result will be a 32-byte boolean, check if it's non-zero
        return !value.equals("0x0000000000000000000000000000000000000000000000000000000000000000");
    }
}