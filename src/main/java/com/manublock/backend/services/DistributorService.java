package com.manublock.backend.services;

import com.manublock.backend.models.*;
import com.manublock.backend.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class DistributorService {

    @Autowired
    private TransportRepository transportRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MaterialRequestRepository materialRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChainRepository chainRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ExtendedBlockchainService blockchainService;

    /**
     * Create a transport for material delivery
     */
    public Transport createMaterialTransport(Long distributorId, Long materialRequestId,
                                             Date scheduledPickupDate, Date scheduledDeliveryDate) {

        Users distributor = userRepository.findById(distributorId)
                .orElseThrow(() -> new RuntimeException("Distributor not found"));

        if (!distributor.getRole().equals(Roles.DISTRIBUTOR)) {
            throw new RuntimeException("User is not a distributor");
        }

        MaterialRequest materialRequest = materialRequestRepository.findById(materialRequestId)
                .orElseThrow(() -> new RuntimeException("Material request not found"));

        if (!materialRequest.getStatus().equals("Allocated")) {
            throw new RuntimeException("Material request is not in 'Allocated' status");
        }

        // Generate tracking number
        String trackingNumber = "MAT-" + Calendar.getInstance().getTimeInMillis();

        // Create transport
        Transport transport = new Transport();
        transport.setTrackingNumber(trackingNumber);
        transport.setDistributor(distributor);
        transport.setSource(materialRequest.getSupplier());
        transport.setDestination(materialRequest.getManufacturer());
        transport.setMaterialRequest(materialRequest);
        transport.setSupplyChain(materialRequest.getSupplyChain());
        transport.setType("Material Transport");
        transport.setStatus("Scheduled");
        transport.setScheduledPickupDate(scheduledPickupDate);
        transport.setScheduledDeliveryDate(scheduledDeliveryDate);
        transport.setCreatedAt(new Date());
        transport.setUpdatedAt(new Date());

        // Update material request status
        materialRequest.setStatus("Ready for Pickup");
        materialRequestRepository.save(materialRequest);

        return transportRepository.save(transport);
    }

    /**
     * Record pickup of items
     */
    public Transport recordPickup(Long transportId) {
        Transport transport = transportRepository.findById(transportId)
                .orElseThrow(() -> new RuntimeException("Transport not found"));

        if (!transport.getStatus().equals("Scheduled")) {
            throw new RuntimeException("Transport is not in 'Scheduled' status");
        }

        // Update status
        transport.setStatus("In Transit");
        transport.setActualPickupDate(new Date());
        transport.setUpdatedAt(new Date());
        transportRepository.save(transport);

        // Update related records
        if (transport.getType().equals("Material Transport")) {
            MaterialRequest materialRequest = transport.getMaterialRequest();
            materialRequest.setStatus("In Transit");
            materialRequestRepository.save(materialRequest);

            // Record blockchain transfer for each item
            materialRequest.getItems().forEach(item -> {
                if (item.getBlockchainItemId() != null) {
                    try {
                        // Get the distributor's user ID instead of wallet address
                        Long distributorId = transport.getDistributor().getId();

                        // Get the supplier's user ID
                        Long supplierId = transport.getSource().getId();

                        // Make the action type more descriptive for tracking
                        String actionType = "material-pickup:distributor:" + distributorId;

                        blockchainService.transferItem(
                                item.getBlockchainItemId(),
                                distributorId,  // to user ID
                                item.getAllocatedQuantity(),
                                actionType,
                                supplierId      // from user ID
                        ).thenAccept(txHash -> {
                            // Store the transaction hash
                            transport.setBlockchainTxHash(txHash);
                            transportRepository.save(transport);
                            System.out.println("Blockchain transfer recorded with hash: " + txHash);
                        }).exceptionally(ex -> {
                            System.err.println("Blockchain transfer failed: " + ex.getMessage());
                            return null;
                        });
                    } catch (Exception e) {
                        System.err.println("Error preparing blockchain transfer: " + e.getMessage());
                    }
                }
            });
        } else if (transport.getType().equals("Product Delivery")) {
            Order order = transport.getOrder();
            order.setStatus("In Transit");
            orderRepository.save(order);

            // Record blockchain transfer for each item
            order.getItems().forEach(item -> {
                if (item.getBlockchainItemId() != null) {
                    try {
                        // IMPORTANT CHANGE: Use customer ID instead of manufacturer ID
                        // This reflects the actual ownership in the blockchain
                        Long customerId = order.getCustomer().getId();
                        Long distributorId = transport.getDistributor().getId();

                        String actionType = "product-pickup:distributor:" + distributorId;

                        // Check if the item exists in the database
                        Optional<Items> itemEntityOpt = itemRepository.findById(item.getBlockchainItemId());
                        if (!itemEntityOpt.isPresent()) {
                            // Create the missing item record if it doesn't exist
                            Items newItem = new Items();
                            newItem.setId(item.getBlockchainItemId());
                            newItem.setName(item.getProduct().getName());
                            newItem.setItemType("product");
                            newItem.setQuantity(item.getQuantity());
                            newItem.setOwner(order.getCustomer());  // Set customer as owner
                            newItem.setSupplyChain(order.getSupplyChain());
                            newItem.setStatus("READY_FOR_SHIPMENT");
                            newItem.setBlockchainStatus("CONFIRMED");
                            newItem.setCreatedAt(new Date());
                            newItem.setUpdatedAt(new Date());
                            itemRepository.save(newItem);
                            System.out.println("Created missing item record for blockchain ID: " + item.getBlockchainItemId());
                        }

                        // Transfer using customer as the source (actual blockchain owner)
                        blockchainService.transferItem(
                                item.getBlockchainItemId(),
                                distributorId,     // to user ID
                                item.getQuantity(),
                                actionType,
                                customerId         // from user ID - CUSTOMER, not manufacturer
                        ).thenAccept(txHash -> {
                            // Store the transaction hash
                            transport.setBlockchainTxHash(txHash);
                            transportRepository.save(transport);
                            System.out.println("Blockchain product pickup recorded with hash: " + txHash);

                            // Update Items table ownership after successful blockchain transfer
                            try {
                                Optional<Items> itemOpt = itemRepository.findById(item.getBlockchainItemId());
                                if (itemOpt.isPresent()) {
                                    Items blockchainItem = itemOpt.get();
                                    blockchainItem.setOwner(transport.getDistributor()); // Transfer ownership to distributor
                                    blockchainItem.setStatus("IN_TRANSIT");
                                    blockchainItem.setUpdatedAt(new Date());
                                    itemRepository.save(blockchainItem);
                                    System.out.println("Database item ownership updated for ID: " + item.getBlockchainItemId());
                                } else {
                                    System.err.println("Could not find item in database with ID: " + item.getBlockchainItemId());
                                }
                            } catch (Exception e) {
                                System.err.println("Error updating item ownership in database: " + e.getMessage());
                            }
                        }).exceptionally(ex -> {
                            System.err.println("Blockchain product pickup failed: " + ex.getMessage());
                            return null;
                        });
                    } catch (Exception e) {
                        System.err.println("Error preparing blockchain product pickup: " + e.getMessage());
                    }
                }
            });
        } else if (transport.getType().equals("Recycling Pickup")) {
            // Handle recycling pickup (if implemented in your system)
            Items item = transport.getRecycledItem();
            if (item != null) {
                try {
                    Long customerId = transport.getSource().getId(); // Customer is the source for recycling
                    Long distributorId = transport.getDistributor().getId();

                    blockchainService.transferItem(
                            item.getId(),
                            distributorId,
                            item.getQuantity(),
                            "recycling-pickup:distributor:" + distributorId,
                            customerId
                    ).thenAccept(txHash -> {
                        transport.setBlockchainTxHash(txHash);
                        transportRepository.save(transport);
                        item.setOwner(transport.getDistributor());
                        item.setStatus("RECYCLING_IN_TRANSIT");
                        item.setBlockchainTxHash(txHash);
                        item.setBlockchainStatus("CONFIRMED");
                        item.setUpdatedAt(new Date());
                        itemRepository.save(item);
                    });
                } catch (Exception e) {
                    System.err.println("Error with recycling pickup blockchain operation: " + e.getMessage());
                }
            }
        }

        return transport;
    }

    /**
     * Record delivery of items
     */
    public Transport recordDelivery(Long transportId) {
        Transport transport = transportRepository.findById(transportId)
                .orElseThrow(() -> new RuntimeException("Transport not found"));

        if (!transport.getStatus().equals("In Transit")) {
            throw new RuntimeException("Transport is not in 'In Transit' status");
        }

        // Update status
        transport.setStatus("Delivered");
        transport.setActualDeliveryDate(new Date());
        transport.setUpdatedAt(new Date());

        // Update related records
        if (transport.getType().equals("Material Transport")) {
            MaterialRequest materialRequest = transport.getMaterialRequest();
            materialRequest.setStatus("Delivered");
            materialRequest.setActualDeliveryDate(new Date());
            materialRequestRepository.save(materialRequest);

            // Record blockchain transfer for each item with null check for wallet address
            materialRequest.getItems().forEach(item -> {
                if (item.getBlockchainItemId() != null) {
                    try {
                        // Get user IDs instead of wallet addresses
                        Long distributorId = transport.getDistributor().getId();
                        Long manufacturerId = transport.getDestination().getId();

                        String actionType = "material-delivery:manufacturer:" + manufacturerId;

                        blockchainService.transferItem(
                                item.getBlockchainItemId(),
                                manufacturerId,   // to user ID
                                item.getAllocatedQuantity(),
                                actionType,
                                distributorId     // from user ID
                        ).thenAccept(txHash -> {
                            transport.setBlockchainTxHash(txHash);
                            transportRepository.save(transport);
                            System.out.println("Blockchain delivery recorded with hash: " + txHash);

                            // Update Items table ownership after successful blockchain transfer
                            try {
                                Optional<Items> itemOpt = itemRepository.findById(item.getBlockchainItemId());
                                if (itemOpt.isPresent()) {
                                    Items blockchainItem = itemOpt.get();
                                    blockchainItem.setOwner(transport.getDestination()); // Transfer ownership to manufacturer
                                    blockchainItem.setStatus("DELIVERED");
                                    blockchainItem.setUpdatedAt(new Date());
                                    itemRepository.save(blockchainItem);
                                    System.out.println("Database item ownership updated for ID: " + item.getBlockchainItemId());
                                } else {
                                    System.err.println("Could not find item in database with ID: " + item.getBlockchainItemId());
                                }
                            } catch (Exception e) {
                                System.err.println("Error updating item ownership in database: " + e.getMessage());
                            }
                        }).exceptionally(ex -> {
                            System.err.println("Blockchain delivery failed: " + ex.getMessage());
                            return null;
                        });
                    } catch (Exception e) {
                        System.err.println("Error preparing blockchain delivery: " + e.getMessage());
                    }
                }
            });
        } else if (transport.getType().equals("Product Delivery")) {
            Order order = transport.getOrder();
            order.setStatus("Delivered");
            orderRepository.save(order);

            // Record blockchain transfer for each item with null check for wallet address
            order.getItems().forEach(item -> {
                if (item.getBlockchainItemId() != null) {
                    try {
                        // Get user IDs
                        Long distributorId = transport.getDistributor().getId();
                        Long customerId = transport.getDestination().getId();

                        String actionType = "product-delivery:customer:" + customerId;

                        blockchainService.transferItem(
                                item.getBlockchainItemId(),
                                customerId,        // to user ID
                                item.getQuantity(),
                                actionType,
                                distributorId      // from user ID
                        ).thenAccept(txHash -> {
                            transport.setBlockchainTxHash(txHash);
                            transportRepository.save(transport);
                            System.out.println("Blockchain delivery recorded with hash: " + txHash);

                            // Update Items table ownership after successful blockchain transfer
                            try {
                                Optional<Items> itemOpt = itemRepository.findById(item.getBlockchainItemId());
                                if (itemOpt.isPresent()) {
                                    Items blockchainItem = itemOpt.get();
                                    blockchainItem.setOwner(transport.getDestination()); // Transfer ownership to customer
                                    blockchainItem.setStatus("DELIVERED");
                                    blockchainItem.setUpdatedAt(new Date());
                                    itemRepository.save(blockchainItem);
                                    System.out.println("Database item ownership updated for ID: " + item.getBlockchainItemId());
                                } else {
                                    System.err.println("Could not find item in database with ID: " + item.getBlockchainItemId());
                                }
                            } catch (Exception e) {
                                System.err.println("Error updating item ownership in database: " + e.getMessage());
                            }
                        }).exceptionally(ex -> {
                            System.err.println("Blockchain delivery failed: " + ex.getMessage());
                            return null;
                        });
                    } catch (Exception e) {
                        System.err.println("Error preparing blockchain delivery: " + e.getMessage());
                    }
                }
            });
        }

        Transport savedTransport = transportRepository.save(transport);
        return savedTransport;
    }

    public List<Transport> getTransportsByDistributor(Long distributorId) {
        return transportRepository.findByDistributor_Id(distributorId);
    }

    public List<MaterialRequest> getReadyMaterialRequests() {
        return materialRequestRepository.findByStatus("Allocated");
    }

    public List<Order> getReadyOrders() {
        return orderRepository.findByStatus("Ready for Shipment");
    }
}