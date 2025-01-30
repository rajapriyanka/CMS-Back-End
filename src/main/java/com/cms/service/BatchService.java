package com.cms.service;

import com.cms.entities.Batch;
import com.cms.repository.BatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BatchService {

    @Autowired
    private BatchRepository batchRepository;

    public Batch registerBatch(Batch batch) {
        return batchRepository.save(batch);
    }

    public Batch updateBatch(Long id, Batch updatedBatch) {
        Batch existingBatch = batchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        if (updatedBatch.getBatchName() != null && !updatedBatch.getBatchName().trim().isEmpty()) {
            existingBatch.setBatchName(updatedBatch.getBatchName().trim());
        }
        if (updatedBatch.getDepartment() != null && !updatedBatch.getDepartment().trim().isEmpty()) {
            existingBatch.setDepartment(updatedBatch.getDepartment().trim());
        }
        if (updatedBatch.getSection() != null) {
            existingBatch.setSection(updatedBatch.getSection().trim());
        }

        return batchRepository.save(existingBatch);
    }

    public void deleteBatch(Long id) {
        batchRepository.deleteById(id);
    }

    public List<Batch> getAllBatches() {
        return batchRepository.findAll();
    }

    public List<Batch> searchBatches(String query) {
        return batchRepository.findByBatchNameContainingOrDepartmentContaining(query, query);
    }
    public List<Batch> searchBatchesByBoth(String batchName, String department) {
        return batchRepository.findByBatchNameContainingIgnoreCaseAndDepartmentContainingIgnoreCase(batchName, department);
    }
}
