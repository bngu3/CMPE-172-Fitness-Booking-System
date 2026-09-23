package com.cmpe172.fitness.service;

import com.cmpe172.fitness.dto.SlotDTO;
import com.cmpe172.fitness.repository.SlotRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service layer: houses business logic. For Milestone 1 this simply
 * delegates to the repository, but this is where filtering/pagination
 * logic (Milestone 2, feature #2) will be added later.
 */
@Service
public class SlotService {

    private final SlotRepository slotRepository;

    public SlotService(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    public List<SlotDTO> getAvailableSlots() {
        return slotRepository.findAvailableSlots();
    }
}
