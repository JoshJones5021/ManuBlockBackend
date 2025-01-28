package com.manublock.backend.services;

import com.manublock.backend.models.SupplyChain;
import com.manublock.backend.repositories.SupplyChainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SupplyChainService {

    @Autowired
    private SupplyChainRepository supplyChainRepository;

    public SupplyChain createSupplyChain(SupplyChain supplyChain) {
        return supplyChainRepository.save(supplyChain);
    }

    public SupplyChain getSupplyChain(Long id) {
        return supplyChainRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supply Chain not found"));
    }

    public List<SupplyChain> getAllSupplyChains() {
        return supplyChainRepository.findAll();
    }

    public SupplyChain updateSupplyChain(Long id, SupplyChain updatedSupplyChain) {
        SupplyChain existing = getSupplyChain(id);
        existing.setName(updatedSupplyChain.getName());
        existing.setDescription(updatedSupplyChain.getDescription());
        existing.setNodes(updatedSupplyChain.getNodes());
        return supplyChainRepository.save(existing);
    }

    public void deleteSupplyChain(Long id) {
        supplyChainRepository.deleteById(id);
    }
}
