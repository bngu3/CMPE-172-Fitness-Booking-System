package com.cmpe172.fitness.service;

import com.cmpe172.fitness.dto.FilterOption;
import com.cmpe172.fitness.dto.SlotDTO;
import com.cmpe172.fitness.repository.SlotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/** Applies availability filters and delegates SQL paging to the repository. */
@Service
public class SlotService {

    private final SlotRepository slotRepository;

    public SlotService(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    public List<SlotDTO> getAvailableSlots(Integer providerId, Integer serviceId,
                                           LocalDate date, int limit, int offset) {
        return slotRepository.findAvailableSlots(providerId, serviceId, date, limit, offset);
    }

    public int countAvailableSlots(Integer providerId, Integer serviceId, LocalDate date) {
        return slotRepository.countAvailableSlots(providerId, serviceId, date);
    }

    public List<FilterOption> getProviderOptions() {
        return slotRepository.findProviderOptions();
    }

    public List<FilterOption> getServiceOptions() {
        return slotRepository.findServiceOptions();
    }
}
