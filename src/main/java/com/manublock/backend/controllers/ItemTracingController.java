package com.manublock.backend.controllers;

import com.manublock.backend.contracts.contract.SmartContract;
import com.manublock.backend.models.Items;
import com.manublock.backend.services.BlockchainService;
import com.manublock.backend.services.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.tuples.generated.Tuple7;
import org.web3j.tuples.generated.Tuple8;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/tracing")
public class ItemTracingController {

    private final ItemService itemService;
    private final BlockchainService blockchainService;

    @Autowired
    public ItemTracingController(ItemService itemService, BlockchainService blockchainService) {
        this.itemService = itemService;
        this.blockchainService = blockchainService;
    }

    @GetMapping("/items/{supplyChainId}")
    public ResponseEntity<?> getItemsBySupplyChain(@PathVariable Long supplyChainId) {
        try {
            List<Items> items = itemService.getItemsBySupplyChain(supplyChainId);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving items: " + e.getMessage());
        }
    }

    @GetMapping("/items/owner/{ownerId}")
    public ResponseEntity<?> getItemsByOwner(@PathVariable Long ownerId) {
        try {
            List<Items> items = itemService.getItemsByOwner(ownerId);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving items: " + e.getMessage());
        }
    }

    @GetMapping("/blockchain/item/{itemId}")
    public ResponseEntity<?> getBlockchainItemDetails(@PathVariable Long itemId) {
        try {
            SmartContract contract = blockchainService.getContract();
            Tuple7<BigInteger, String, BigInteger, BigInteger, BigInteger, String, Boolean> itemDetails =
                    contract.getItemDetails(BigInteger.valueOf(itemId)).send();

            Map<String, Object> response = new HashMap<>();
            response.put("id", itemDetails.component1().longValue());
            response.put("owner", itemDetails.component2());
            response.put("quantity", itemDetails.component3().longValue());
            response.put("supplyChainId", itemDetails.component4().longValue());
            response.put("status", itemDetails.component5().intValue());
            response.put("itemType", itemDetails.component6());
            response.put("isActive", itemDetails.component7());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving blockchain item details: " + e.getMessage());
        }
    }

    @GetMapping("/blockchain/item/{itemId}/parents")
    public ResponseEntity<?> getBlockchainItemParents(@PathVariable Long itemId) {
        try {
            SmartContract contract = blockchainService.getContract();
            List<BigInteger> parentIds = contract.getItemParents(BigInteger.valueOf(itemId)).send();

            List<Long> response = new ArrayList<>();
            for (BigInteger parentId : parentIds) {
                response.add(parentId.longValue());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving blockchain item parents: " + e.getMessage());
        }
    }

    @GetMapping("/blockchain/item/{itemId}/children")
    public ResponseEntity<?> getBlockchainItemChildren(@PathVariable Long itemId) {
        try {
            SmartContract contract = blockchainService.getContract();
            List<BigInteger> childIds = contract.getItemChildren(BigInteger.valueOf(itemId)).send();

            List<Long> response = new ArrayList<>();
            for (BigInteger childId : childIds) {
                response.add(childId.longValue());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving blockchain item children: " + e.getMessage());
        }
    }

    @GetMapping("/blockchain/transaction/{transactionId}")
    public ResponseEntity<?> getBlockchainTransactionDetails(@PathVariable Long transactionId) {
        try {
            SmartContract contract = blockchainService.getContract();
            Tuple8<BigInteger, BigInteger, String, String, BigInteger, BigInteger, BigInteger, String> txDetails =
                    contract.getTransactionDetails(BigInteger.valueOf(transactionId)).send();

            Map<String, Object> response = new HashMap<>();
            response.put("id", txDetails.component1().longValue());
            response.put("itemId", txDetails.component2().longValue());
            response.put("from", txDetails.component3());
            response.put("to", txDetails.component4());
            response.put("supplyChainId", txDetails.component5().longValue());
            response.put("quantityTransferred", txDetails.component6().longValue());
            response.put("timestamp", txDetails.component7().longValue());
            response.put("actionType", txDetails.component8());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving blockchain transaction details: " + e.getMessage());
        }
    }

    @GetMapping("/item/{itemId}/trace")
    public ResponseEntity<?> traceItemHistory(@PathVariable Long itemId) {
        try {
            Map<String, Object> response = new HashMap<>();

            // Get blockchain item details
            SmartContract contract = blockchainService.getContract();
            Tuple7<BigInteger, String, BigInteger, BigInteger, BigInteger, String, Boolean> itemDetails =
                    contract.getItemDetails(BigInteger.valueOf(itemId)).send();

            Map<String, Object> currentItem = new HashMap<>();
            currentItem.put("id", itemDetails.component1().longValue());
            currentItem.put("owner", itemDetails.component2());
            currentItem.put("quantity", itemDetails.component3().longValue());
            currentItem.put("supplyChainId", itemDetails.component4().longValue());
            currentItem.put("status", itemDetails.component5().intValue());
            currentItem.put("itemType", itemDetails.component6());
            currentItem.put("isActive", itemDetails.component7());

            response.put("currentState", currentItem);

            // Get parent items (recursively if needed)
            List<BigInteger> parentIds = contract.getItemParents(BigInteger.valueOf(itemId)).send();
            List<Map<String, Object>> parentItems = new ArrayList<>();

            for (BigInteger parentId : parentIds) {
                Tuple7<BigInteger, String, BigInteger, BigInteger, BigInteger, String, Boolean> parentDetails =
                        contract.getItemDetails(parentId).send();

                Map<String, Object> parentItem = new HashMap<>();
                parentItem.put("id", parentDetails.component1().longValue());
                parentItem.put("owner", parentDetails.component2());
                parentItem.put("quantity", parentDetails.component3().longValue());
                parentItem.put("supplyChainId", parentDetails.component4().longValue());
                parentItem.put("status", parentDetails.component5().intValue());
                parentItem.put("itemType", parentDetails.component6());
                parentItem.put("isActive", parentDetails.component7());

                parentItems.add(parentItem);
            }

            response.put("parentItems", parentItems);

            // Get child items (derivatives or next in supply chain)
            List<BigInteger> childIds = contract.getItemChildren(BigInteger.valueOf(itemId)).send();
            List<Map<String, Object>> childItems = new ArrayList<>();

            for (BigInteger childId : childIds) {
                Tuple7<BigInteger, String, BigInteger, BigInteger, BigInteger, String, Boolean> childDetails =
                        contract.getItemDetails(childId).send();

                Map<String, Object> childItem = new HashMap<>();
                childItem.put("id", childDetails.component1().longValue());
                childItem.put("owner", childDetails.component2());
                childItem.put("quantity", childDetails.component3().longValue());
                childItem.put("supplyChainId", childDetails.component4().longValue());
                childItem.put("status", childDetails.component5().intValue());
                childItem.put("itemType", childDetails.component6());
                childItem.put("isActive", childDetails.component7());

                childItems.add(childItem);
            }

            response.put("childItems", childItems);

            // Get database details if available
            try {
                Items dbItem = itemService.getItemById(itemId);
                if (dbItem != null) {
                    Map<String, Object> dbDetails = new HashMap<>();
                    dbDetails.put("name", dbItem.getName());
                    dbDetails.put("owner", dbItem.getOwner().getUsername());
                    dbDetails.put("createdAt", dbItem.getCreatedAt());
                    dbDetails.put("updatedAt", dbItem.getUpdatedAt());
                    response.put("databaseDetails", dbDetails);
                }
            } catch (Exception e) {
                // Database item may not exist, continue without it
                response.put("databaseDetails", "Item not found in database");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error tracing item history: " + e.getMessage());
        }
    }
}