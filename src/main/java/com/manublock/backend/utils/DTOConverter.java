package com.manublock.backend.utils;

import com.manublock.backend.dto.OrderItemResponseDTO;
import com.manublock.backend.dto.OrderResponseDTO;
import com.manublock.backend.models.Order;
import com.manublock.backend.models.OrderItem;

import java.util.List;
import java.util.stream.Collectors;

public class DTOConverter {

    public static OrderResponseDTO convertToOrderDTO(Order order) {
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

        // Set customer info
        if (order.getCustomer() != null) {
            dto.setCustomerId(order.getCustomer().getId());
            dto.setCustomerName(order.getCustomer().getUsername());
        }

        // Set supply chain info
        if (order.getSupplyChain() != null) {
            dto.setSupplyChainId(order.getSupplyChain().getId());
            dto.setSupplyChainName(order.getSupplyChain().getName());
        }

        // Convert order items
        if (order.getItems() != null) {
            List<OrderItemResponseDTO> itemDTOs = order.getItems().stream()
                    .map(DTOConverter::convertToOrderItemDTO)
                    .collect(Collectors.toList());

            dto.setItems(itemDTOs);
        }

        return dto;
    }

    public static OrderItemResponseDTO convertToOrderItemDTO(OrderItem item) {
        OrderItemResponseDTO itemDTO = new OrderItemResponseDTO();
        itemDTO.setId(item.getId());

        if (item.getPrice() != null) {
            itemDTO.setPrice(item.getPrice().doubleValue());
        }

        itemDTO.setQuantity(item.getQuantity());
        itemDTO.setStatus(item.getStatus());

        // Set product info
        if (item.getProduct() != null) {
            itemDTO.setProductId(item.getProduct().getId());
            itemDTO.setProductName(item.getProduct().getName());
        }

        return itemDTO;
    }
}