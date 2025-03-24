package com.manublock.backend.utils;

import com.manublock.backend.dto.OrderItemResponseDTO;
import com.manublock.backend.dto.OrderResponseDTO;
import com.manublock.backend.models.Order;
import com.manublock.backend.models.OrderItem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DTOConverter {

    /**
     * Convert an Order entity to OrderResponseDTO without circular references
     */
    public static OrderResponseDTO convertToOrderDTO(Order order) {
        if (order == null) {
            return null;
        }

        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setRequestedDeliveryDate(order.getRequestedDeliveryDate());
        dto.setActualDeliveryDate(order.getActualDeliveryDate());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setDeliveryNotes(order.getDeliveryNotes());
        dto.setBlockchainTxHash(order.getBlockchainTxHash());

        // Set customer info safely
        if (order.getCustomer() != null) {
            dto.setCustomerId(order.getCustomer().getId());
            dto.setCustomerName(order.getCustomer().getUsername());
        }

        // Set supply chain info safely
        if (order.getSupplyChain() != null) {
            dto.setSupplyChainId(order.getSupplyChain().getId());
            dto.setSupplyChainName(order.getSupplyChain().getName());
        }

        // Convert order items safely
        if (order.getItems() != null) {
            List<OrderItemResponseDTO> itemDTOs = new ArrayList<>();
            for (OrderItem item : order.getItems()) {
                itemDTOs.add(convertToOrderItemDTO(item));
            }
            dto.setItems(itemDTOs);
        } else {
            dto.setItems(new ArrayList<>());
        }

        return dto;
    }

    /**
     * Convert an OrderItem entity to OrderItemResponseDTO without circular references
     */
    public static OrderItemResponseDTO convertToOrderItemDTO(OrderItem item) {
        if (item == null) {
            return null;
        }

        OrderItemResponseDTO itemDTO = new OrderItemResponseDTO();
        itemDTO.setId(item.getId());

        if (item.getPrice() != null) {
            itemDTO.setPrice(item.getPrice().doubleValue());
        }

        itemDTO.setQuantity(item.getQuantity());
        itemDTO.setStatus(item.getStatus());

        // Set product info safely
        if (item.getProduct() != null) {
            itemDTO.setProductId(item.getProduct().getId());
            itemDTO.setProductName(item.getProduct().getName());
        }

        return itemDTO;
    }

    /**
     * Convert a list of Order entities to OrderResponseDTOs
     */
    public static List<OrderResponseDTO> convertToOrderDTOList(List<Order> orders) {
        if (orders == null) {
            return new ArrayList<>();
        }

        return orders.stream()
                .map(DTOConverter::convertToOrderDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert a list of OrderItem entities to OrderItemResponseDTOs
     */
    public static List<OrderItemResponseDTO> convertToOrderItemDTOList(List<OrderItem> items) {
        if (items == null) {
            return new ArrayList<>();
        }

        return items.stream()
                .map(DTOConverter::convertToOrderItemDTO)
                .collect(Collectors.toList());
    }
}