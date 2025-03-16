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
    private ItemService itemService;

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
     * Create a transport for product delivery
     */
    public Transport createProductTransport(Long distributorId, Long orderId,
                                            Date scheduledPickupDate, Date scheduledDeliveryDate) {

        Users distributor = userRepository.findById(distributorId)
                .orElseThrow(() -> new RuntimeException("Distributor not found"));

        if (!distributor.getRole().equals(Roles.DISTRIBUTOR)) {
            throw new RuntimeException("User is not a distributor");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getStatus().equals("Ready for Shipment")) {
            throw new RuntimeException("Order is not in 'Ready for Shipment' status");
        }

        // Get manufacturer from first order item's product
        Users manufacturer = order.getItems().get(0).getProduct().getManufacturer();

        // Generate tracking number
        String trackingNumber = "PRD-" + Calendar.getInstance().getTimeInMillis();

        // Create transport
        Transport transport = new Transport();
        transport.setTrackingNumber(trackingNumber);
        transport.setDistributor(distributor);
        transport.setSource(manufacturer);
        transport.setDestination(order.getCustomer());
        transport.setOrder(order);
        transport.setSupplyChain(order.getSupplyChain());
        transport.setType("Product Delivery");
        transport.setStatus("Scheduled");
        transport.setPickupLocation(manufacturer.getUsername() + "'s Facility");
        transport.setDeliveryLocation(order.getShippingAddress());
        transport.setScheduledPickupDate(scheduledPickupDate);
        transport.setScheduledDeliveryDate(scheduledDeliveryDate);
        transport.setCreatedAt(new Date());
        transport.setUpdatedAt(new Date());

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
                        // Get user IDs
                        Long manufacturerId = item.getProduct().getManufacturer().getId();
                        Long distributorId = transport.getDistributor().getId();

                        blockchainService.transferItem(
                                item.getBlockchainItemId(),
                                distributorId,     // to user ID
                                item.getQuantity(),
                                "product-pickup",
                                manufacturerId     // from user ID
                        );
                    } catch (Exception e) {
                        System.err.println("Error preparing blockchain transfer: " + e.getMessage());
                    }
                }
            });
        }

        return transportRepository.save(transport);
    }

    /**
     * Record delivery of items
     */
    // Update the recordDelivery method in DistributorService.java
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

    // GET methods

    public List<Transport> getTransportsByDistributor(Long distributorId) {
        return transportRepository.findByDistributor_Id(distributorId);
    }

    public List<Transport> getTransportsByDistributorAndStatus(Long distributorId, String status) {
        return transportRepository.findByDistributor_IdAndStatus(distributorId, status);
    }

    public List<Transport> getTransportsByType(Long distributorId, String type) {
        return transportRepository.findByDistributor_IdAndType(distributorId, type);
    }

    public List<Transport> getTransportsBySource(Long sourceId) {
        return transportRepository.findBySource_Id(sourceId);
    }

    public List<MaterialRequest> getReadyMaterialRequests() {
        return materialRequestRepository.findByStatus("Allocated");
    }

    public List<Order> getReadyOrders() {
        return orderRepository.findByStatus("Ready for Shipment");
    }
}