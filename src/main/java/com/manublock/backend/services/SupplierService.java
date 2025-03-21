package com.manublock.backend.services;

import com.manublock.backend.models.*;
import com.manublock.backend.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Random;

@Service
public class SupplierService {

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MaterialRequestRepository materialRequestRepository;

    @Autowired
    private MaterialRequestItemRepository materialRequestItemRepository;

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChainRepository chainRepository;

    @Autowired
    private ExtendedBlockchainService blockchainService;

    @Autowired
    private ItemRepository itemRepository;

    /**
     * Create a new material in both database and blockchain
     */
    public Material createMaterial(String name, String description, Long quantity, String unit,
                                   String specifications, Long supplierId, Long supplyChainId) {

        Users supplier = userRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        if (!supplier.getRole().equals(Roles.SUPPLIER)) {
            throw new RuntimeException("User is not a supplier");
        }

        Chains supplyChain = chainRepository.findById(supplyChainId)
                .orElseThrow(() -> new RuntimeException("Supply chain not found"));

        // Generate a unique ID for blockchain
        Long blockchainItemId = generateUniqueBlockchainId();

        // Create material in database
        Material material = new Material();
        material.setName(name);
        material.setDescription(description);
        material.setQuantity(quantity);
        material.setUnit(unit);
        material.setSpecifications(specifications);
        material.setSupplier(supplier);
        material.setActive(true);
        material.setBlockchainItemId(blockchainItemId);
        material.setCreatedAt(new Date());
        material.setUpdatedAt(new Date());

        Material savedMaterial = materialRepository.save(material);

        // Create the item on blockchain
        itemService.createItem(
                blockchainItemId,
                name,
                "raw-material",
                quantity,
                supplierId,
                supplyChainId
        );

        return savedMaterial;
    }

    /**
     * Approve a material request from a manufacturer
     */
    public MaterialRequest approveRequest(Long requestId, List<MaterialRequestItemApproval> approvals) {
        MaterialRequest request = materialRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Material request not found"));

        if (!request.getStatus().equals("Requested")) {
            throw new RuntimeException("Request is not in 'Requested' status");
        }

        // Update each item with approved quantity
        for (MaterialRequestItemApproval approval : approvals) {
            MaterialRequestItem item = materialRequestItemRepository.findById(approval.getItemId())
                    .orElseThrow(() -> new RuntimeException("Request item not found"));

            if (approval.getApprovedQuantity() > item.getRequestedQuantity()) {
                throw new RuntimeException("Approved quantity cannot exceed requested quantity");
            }

            item.setApprovedQuantity(approval.getApprovedQuantity());
            item.setStatus("Approved");
            materialRequestItemRepository.save(item);
        }

        // Update request status
        request.setStatus("Approved");
        request.setUpdatedAt(new Date());

        return materialRequestRepository.save(request);
    }

    /**
     * Allocate materials for an approved request
     * This will create blockchain items for the allocated materials
     */
    public MaterialRequest allocateMaterials(Long requestId) {
        MaterialRequest request = materialRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Material request not found"));

        if (!request.getStatus().equals("Approved")) {
            throw new RuntimeException("Request is not in 'Approved' status");
        }

        // Process each approved item
        for (MaterialRequestItem item : request.getItems()) {
            if (!item.getStatus().equals("Approved")) {
                continue; // Skip items that weren't approved
            }

            Material material = item.getMaterial();
            Long approvedQuantity = item.getApprovedQuantity();

            // Check if we have enough quantity
            if (material.getQuantity() < approvedQuantity) {
                throw new RuntimeException("Insufficient quantity for material: " + material.getName());
            }

            // Update material quantity
            material.setQuantity(material.getQuantity() - approvedQuantity);
            materialRepository.save(material);

            // Generate blockchain ID for the allocated material
            Long blockchainItemId = generateUniqueBlockchainId();

            // Update the request item with blockchain ID and status
            item.setAllocatedQuantity(approvedQuantity);
            item.setStatus("Allocated");
            item.setBlockchainItemId(blockchainItemId);
            materialRequestItemRepository.save(item);

            // Create the allocated item on blockchain
            // This will create a new item with the material as its parent
            List<Long> sourceItemIds = List.of(material.getBlockchainItemId());
            List<Long> inputQuantities = List.of(approvedQuantity);

            // Use blockchain processing to maintain parent-child relationship
            blockchainService.processItem(
                    sourceItemIds,
                    blockchainItemId,
                    inputQuantities,
                    approvedQuantity,
                    "allocated-material",
                    request.getSupplier().getId()  // User ID of the processor (supplier)
            ).thenAccept(txHash -> {
                // Create a corresponding record in the Items table
                Items allocatedItem = new Items();
                allocatedItem.setId(blockchainItemId);
                allocatedItem.setName(material.getName());
                allocatedItem.setItemType("allocated-material");
                allocatedItem.setQuantity(approvedQuantity);
                allocatedItem.setOwner(request.getSupplier());
                allocatedItem.setSupplyChain(request.getSupplyChain());
                allocatedItem.setStatus("CREATED");
                allocatedItem.setParentItemIds(List.of(material.getBlockchainItemId()));
                allocatedItem.setBlockchainTxHash(txHash);
                allocatedItem.setBlockchainStatus("CONFIRMED");
                allocatedItem.setCreatedAt(new Date());
                allocatedItem.setUpdatedAt(new Date());

                // Save to items table using repository
                itemRepository.save(allocatedItem);
            });
        }

        // Update request status
        request.setStatus("Allocated");
        request.setUpdatedAt(new Date());

        return materialRequestRepository.save(request);
    }

    /**
     * Generate a unique ID for blockchain items
     */
    private Long generateUniqueBlockchainId() {
        // Simple implementation - in production you might want something more sophisticated
        Random random = new Random();
        long id = random.nextLong(1_000_000_000, 9_999_999_999L);
        return id;
    }

    // GET methods

    public List<Material> getMaterialsBySupplier(Long supplierId) {
        return materialRepository.findBySupplier_Id(supplierId);
    }

    public List<Material> getActiveMaterialsBySupplier(Long supplierId) {
        return materialRepository.findByActiveTrueAndSupplier_Id(supplierId);
    }

    public List<MaterialRequest> getPendingRequests(Long supplierId) {
        return materialRequestRepository.findBySupplier_IdAndStatus(supplierId, "Requested");
    }

    public List<MaterialRequest> getRequestsByStatus(Long supplierId, String status) {
        return materialRequestRepository.findBySupplier_IdAndStatus(supplierId, status);
    }

    // Helper class for approving material requests
    public static class MaterialRequestItemApproval {
        private Long itemId;
        private Long approvedQuantity;

        public Long getItemId() {
            return itemId;
        }

        public void setItemId(Long itemId) {
            this.itemId = itemId;
        }

        public Long getApprovedQuantity() {
            return approvedQuantity;
        }

        public void setApprovedQuantity(Long approvedQuantity) {
            this.approvedQuantity = approvedQuantity;
        }
    }
}