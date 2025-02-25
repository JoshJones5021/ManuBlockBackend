package com.manublock.backend.services;

import com.manublock.backend.models.SupplyChainNode;
import com.manublock.backend.repositories.NodeRepository;
import com.manublock.backend.repositories.SupplyChainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NodeService {

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private SupplyChainRepository supplyChainRepository;

    public SupplyChainNode addNode(Long supplyChainId, SupplyChainNode node) {
        node.setSupplyChain(supplyChainRepository.findById(supplyChainId)
                .orElseThrow(() -> new RuntimeException("Supply Chain not found")));
        return nodeRepository.save(node);
    }

    public List<SupplyChainNode> getNodesBySupplyChainId(Long supplyChainId) {
        return nodeRepository.findBySupplyChainId(supplyChainId);
    }

    public SupplyChainNode updateNode(Long nodeId, SupplyChainNode updatedNode) {
        SupplyChainNode existingNode = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node not found"));

        existingNode.setName(updatedNode.getName());
        existingNode.setRole(updatedNode.getRole());
        existingNode.setAssignedUser(updatedNode.getAssignedUser());
        existingNode.setStatus(updatedNode.getStatus());
        existingNode.setX(updatedNode.getX());
        existingNode.setY(updatedNode.getY());

        return nodeRepository.save(existingNode);
    }

    public void deleteNode(Long nodeId) {
        nodeRepository.deleteById(nodeId);
    }
}