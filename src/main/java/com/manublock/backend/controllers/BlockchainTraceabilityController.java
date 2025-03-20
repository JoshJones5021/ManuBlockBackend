package com.manublock.backend.controllers;

import com.manublock.backend.models.BlockchainTransaction;
import com.manublock.backend.models.Items;
import com.manublock.backend.repositories.BlockchainTransactionRepository;
import com.manublock.backend.repositories.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * New endpoints to be added to your existing BlockchainController
 */
@RestController
@RequestMapping("/api/blockchain")
public class BlockchainTraceabilityController {

    @Autowired
    private BlockchainTransactionRepository transactionRepository;

    @Autowired
    private ItemRepository itemRepository;

    /**
     * Get blockchain transactions filtered by supply chain ID
     */
    @GetMapping("/transactions/supply-chain/{supplyChainId}")
    public ResponseEntity<?> getTransactionsBySupplyChain(@PathVariable Long supplyChainId) {
        try {
            List<BlockchainTransaction> transactions = transactionRepository.findAll();

            // Filter transactions related to this supply chain
            List<BlockchainTransaction> filteredTransactions = transactions.stream()
                    .filter(tx -> isTransactionRelatedToSupplyChain(tx, supplyChainId))
                    .collect(Collectors.toList());

            // Convert to response DTOs with enhanced information
            List<Map<String, Object>> responseDtos = filteredTransactions.stream()
                    .map(tx -> enrichTransactionData(tx))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responseDtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving blockchain transactions: " + e.getMessage()));
        }
    }

    /**
     * Get transaction timeline for a specific item
     */
    @GetMapping("/transactions/item-timeline/{itemId}")
    public ResponseEntity<?> getItemTransactionTimeline(@PathVariable Long itemId) {
        try {
            // Get the item
            Optional<Items> itemOpt = itemRepository.findById(itemId);
            if (itemOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Item not found with ID: " + itemId));
            }

            Items item = itemOpt.get();

            // Find all transactions related to this item
            List<BlockchainTransaction> allTransactions = transactionRepository.findAll();

            // Filter for transactions involving this item
            List<BlockchainTransaction> itemTransactions = allTransactions.stream()
                    .filter(tx -> isTransactionRelatedToItem(tx, itemId))
                    .collect(Collectors.toList());

            // Sort by timestamp
            itemTransactions.sort(Comparator.comparing(BlockchainTransaction::getCreatedAt));

            // Convert to response DTOs with enhanced information
            List<Map<String, Object>> timeline = itemTransactions.stream()
                    .map(tx -> enrichTransactionData(tx))
                    .collect(Collectors.toList());

            // Add parent information if available
            if (item.getParentItemIds() != null && !item.getParentItemIds().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("item", item);
                response.put("timeline", timeline);
                response.put("parentIds", item.getParentItemIds());

                // Get basic info about parent items
                List<Map<String, Object>> parentInfo = new ArrayList<>();
                for (Long parentId : item.getParentItemIds()) {
                    Optional<Items> parentOpt = itemRepository.findById(parentId);
                    if (parentOpt.isPresent()) {
                        Items parent = parentOpt.get();
                        Map<String, Object> info = new HashMap<>();
                        info.put("id", parent.getId());
                        info.put("name", parent.getName());
                        info.put("type", parent.getItemType());
                        info.put("status", parent.getStatus());
                        parentInfo.add(info);
                    }
                }
                response.put("parents", parentInfo);

                return ResponseEntity.ok(response);
            }

            return ResponseEntity.ok(Map.of(
                    "item", item,
                    "timeline", timeline
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving item timeline: " + e.getMessage()));
        }
    }

    /**
     * Check if a transaction is related to a specific supply chain
     */
    private boolean isTransactionRelatedToSupplyChain(BlockchainTransaction tx, Long supplyChainId) {
        // Check parameters string first
        if (tx.getParameters() != null) {
            String params = tx.getParameters();
            String supplyChainIdStr = supplyChainId.toString();

            // Different patterns based on function type
            if (tx.getFunction().equals("createSupplyChain")) {
                // For createSupplyChain, the first parameter is typically the supplyChainId
                return params.startsWith(supplyChainIdStr + ",") || params.equals(supplyChainIdStr);
            } else if (tx.getFunction().equals("authorizeParticipant")) {
                // For authorizeParticipant, the format is usually "supplyChainId,userId"
                return params.startsWith(supplyChainIdStr + ",");
            } else {
                // For other functions, check for the supplyChainId in the parameters
                // The format is often "param1,param2,supplyChainId,..." or similar
                String[] paramParts = params.split(",");
                for (String part : paramParts) {
                    if (part.trim().equals(supplyChainIdStr)) {
                        return true;
                    }
                }

                // Also check for direct mentions
                return params.contains("supplyChainId=" + supplyChainIdStr) ||
                        params.contains("\"supplyChainId\":" + supplyChainIdStr);
            }
        }

        return false;
    }

    /**
     * Check if a transaction is related to a specific item
     */
    private boolean isTransactionRelatedToItem(BlockchainTransaction tx, Long itemId) {
        if (tx.getParameters() != null) {
            String params = tx.getParameters();
            String itemIdStr = itemId.toString();

            // Different patterns based on function type
            if (tx.getFunction().equals("createItem")) {
                // For createItem, the first parameter is typically the itemId
                return params.startsWith(itemIdStr + ",");
            } else if (tx.getFunction().equals("transferItem") || tx.getFunction().equals("updateItemStatus")) {
                // For these functions, the first parameter is also usually the itemId
                return params.startsWith(itemIdStr + ",");
            } else if (tx.getFunction().equals("processItem")) {
                // For processItem, check both source items and new item
                return params.contains("[" + itemIdStr + ",") ||
                        params.contains("," + itemIdStr + ",") ||
                        params.contains("," + itemIdStr + "]");
            }

            // Generic check for itemId in parameters
            return params.contains("itemId=" + itemIdStr) ||
                    params.contains("\"itemId\":" + itemIdStr);
        }

        return false;
    }

    /**
     * Enrich transaction data with human-readable information
     */
    private Map<String, Object> enrichTransactionData(BlockchainTransaction tx) {
        Map<String, Object> enriched = new HashMap<>();

        // Basic transaction info
        enriched.put("id", tx.getId());
        enriched.put("function", tx.getFunction());
        enriched.put("status", tx.getStatus());
        enriched.put("txHash", tx.getTransactionHash());
        enriched.put("createdAt", tx.getCreatedAt());
        enriched.put("confirmedAt", tx.getConfirmedAt());

        // Parse and enhance parameters
        if (tx.getParameters() != null) {
            enriched.put("rawParameters", tx.getParameters());

            // Add human-readable description based on function type
            enriched.put("description", generateHumanReadableDescription(tx));

            // Extract related entities
            Map<String, Object> relatedEntities = extractRelatedEntities(tx);
            if (!relatedEntities.isEmpty()) {
                enriched.put("relatedEntities", relatedEntities);
            }
        }

        return enriched;
    }

    /**
     * Generate a human-readable description of the transaction
     */
    private String generateHumanReadableDescription(BlockchainTransaction tx) {
        String function = tx.getFunction();
        String params = tx.getParameters();

        if (params == null) {
            return "Unknown transaction: " + function;
        }

        String[] paramParts = params.split(",");

        switch (function) {
            case "createSupplyChain":
                if (paramParts.length >= 1) {
                    return "Created supply chain with ID: " + paramParts[0];
                }
                break;

            case "authorizeParticipant":
                if (paramParts.length >= 2) {
                    return "Authorized user " + paramParts[1] + " on supply chain " + paramParts[0];
                }
                break;

            case "createItem":
                if (paramParts.length >= 5) {
                    return "Created " + paramParts[3] + " item (ID: " + paramParts[0] +
                            ") with quantity " + paramParts[2] + " by user " + paramParts[4];
                }
                break;

            case "transferItem":
                if (paramParts.length >= 5) {
                    // Extract action type from parameter 4
                    String actionType = paramParts[3];

                    if (actionType.contains("recycling-pickup")) {
                        return "Item " + paramParts[0] + " collected for recycling by distributor (user " +
                                paramParts[1] + ") from customer " + paramParts[4];
                    } else if (actionType.contains("recycling-delivery")) {
                        return "Item " + paramParts[0] + " delivered to manufacturer (user " +
                                paramParts[1] + ") for recycling processing";
                    } else {
                        return "Transferred item " + paramParts[0] + " to user " +
                                paramParts[1] + " (quantity: " + paramParts[2] + ") from user " + paramParts[4];
                    }
                }
                break;

            case "processItem":
                if (paramParts.length >= 5) {
                    String sourceIds = paramParts[0].replace("[", "").replace("]", "");
                    String outputItemId = paramParts[1];
                    String outputType = paramParts[4];
                    return "Processed source items [" + sourceIds + "] into new " +
                            outputType + " item with ID: " + outputItemId;
                }
                break;

            case "updateItemStatus":
                if (paramParts.length >= 3) {
                    int statusCode = Integer.parseInt(paramParts[1]);

                    // Check if this is actually a recycling-related status update
                    if (statusCode == 4) {
                        // Status code 4 is being used for recycling in this context
                        return "Marked item " + paramParts[0] + " for recycling";
                    } else {
                        return "Updated status of item " + paramParts[0] + " to " +
                                getStatusName(statusCode);
                    }
                }
                break;
        }

        return "Transaction: " + function + " with parameters: " + params;
    }

    /**
     * Convert numeric status to string
     */
    private String getStatusName(int status) {
        switch (status) {
            case 0: return "CREATED";
            case 1: return "IN_TRANSIT";
            case 2: return "PROCESSING";
            case 3: return "COMPLETED";
            case 4: return "REJECTED";
            case 5: return "CHURNED";
            case 6: return "RECYCLED";
            default: return "UNKNOWN (" + status + ")";
        }
    }

    /**
     * Extract related entities from transaction parameters
     */
    private Map<String, Object> extractRelatedEntities(BlockchainTransaction tx) {
        Map<String, Object> entities = new HashMap<>();
        String function = tx.getFunction();
        String params = tx.getParameters();

        if (params == null) {
            return entities;
        }

        String[] paramParts = params.split(",");

        // Extract based on function type
        switch (function) {
            case "createSupplyChain":
                if (paramParts.length >= 1) {
                    entities.put("supplyChainId", Long.parseLong(paramParts[0]));
                }
                break;

            case "authorizeParticipant":
                if (paramParts.length >= 2) {
                    entities.put("supplyChainId", Long.parseLong(paramParts[0]));
                    entities.put("userId", Long.parseLong(paramParts[1]));
                }
                break;

            case "createItem":
                if (paramParts.length >= 5) {
                    entities.put("itemId", Long.parseLong(paramParts[0]));
                    entities.put("supplyChainId", Long.parseLong(paramParts[1]));
                    entities.put("itemType", paramParts[3]);
                    entities.put("creatorId", Long.parseLong(paramParts[4]));
                }
                break;

            case "transferItem":
                if (paramParts.length >= 5) {
                    entities.put("itemId", Long.parseLong(paramParts[0]));
                    entities.put("toUserId", Long.parseLong(paramParts[1]));
                    entities.put("quantity", Long.parseLong(paramParts[2]));
                    entities.put("actionType", paramParts[3]);
                    entities.put("fromUserId", Long.parseLong(paramParts[4]));
                }
                break;

            case "processItem":
                if (paramParts.length >= 6) {
                    // Parse source item IDs - handle array format [id1,id2,...]
                    String sourceIdsStr = paramParts[0];
                    List<Long> sourceIds = new ArrayList<>();

                    // Simple array parsing
                    sourceIdsStr = sourceIdsStr.replace("[", "").replace("]", "");
                    for (String idStr : sourceIdsStr.split(",")) {
                        if (!idStr.trim().isEmpty()) {
                            sourceIds.add(Long.parseLong(idStr.trim()));
                        }
                    }

                    entities.put("sourceItemIds", sourceIds);
                    entities.put("newItemId", Long.parseLong(paramParts[1]));
                    entities.put("newItemType", paramParts[4]);
                    entities.put("processorId", Long.parseLong(paramParts[5]));
                }
                break;

            case "updateItemStatus":
                if (paramParts.length >= 3) {
                    entities.put("itemId", Long.parseLong(paramParts[0]));
                    entities.put("status", Integer.parseInt(paramParts[1]));
                    entities.put("ownerId", Long.parseLong(paramParts[2]));
                }
                break;
        }

        return entities;
    }

    /**
     * Get all blockchain transactions without any filtering
     */
    @GetMapping("/transactions/all")
    public ResponseEntity<?> getAllTransactions() {
        try {
            List<BlockchainTransaction> transactions = transactionRepository.findAll();

            // Convert to response DTOs with enhanced information
            List<Map<String, Object>> responseDtos = transactions.stream()
                    .map(tx -> enrichTransactionData(tx))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responseDtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving blockchain transactions: " + e.getMessage()));
        }
    }
}