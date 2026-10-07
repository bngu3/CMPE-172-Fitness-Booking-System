package com.cmpe172.fitness.service;

import com.cmpe172.fitness.dto.FilterOption;
import com.cmpe172.fitness.dto.ProviderAppointmentDTO;
import com.cmpe172.fitness.dto.ProviderSlotDTO;
import com.cmpe172.fitness.exception.InvalidAvailabilityRequestException;
import com.cmpe172.fitness.exception.InvalidBookingRequestException;
import com.cmpe172.fitness.exception.ResourceNotFoundException;
import com.cmpe172.fitness.exception.SlotUnavailableException;
import com.cmpe172.fitness.repository.ProviderRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ProviderService {

    private final ProviderRepository providerRepository;

    public ProviderService(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    public List<FilterOption> getServices(String providerEmail) {
        return providerRepository.findServices(providerId(providerEmail));
    }

    public List<ProviderSlotDTO> getAvailability(String providerEmail) {
        return providerRepository.findSlots(providerId(providerEmail));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void createSlot(String providerEmail, int serviceId, LocalDate date,
                           LocalTime start, LocalTime end) {
        if (serviceId <= 0 || date == null || start == null || end == null
                || date.isBefore(LocalDate.now()) || !start.isBefore(end)
                || (date.isEqual(LocalDate.now()) && !start.isAfter(LocalTime.now()))) {
            throw new InvalidAvailabilityRequestException();
        }

        int providerId = providerId(providerEmail);
        try {
            providerRepository.insertSlot(providerId, serviceId, date, start, end)
                    .orElseThrow(() -> new ResourceNotFoundException("Service not found for this provider."));
        } catch (DuplicateKeyException exception) {
            throw new SlotUnavailableException();
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteSlot(String providerEmail, int slotId) {
        if (slotId <= 0) {
            throw new InvalidBookingRequestException();
        }

        int providerId = providerId(providerEmail);
        ProviderRepository.OwnedSlot slot = providerRepository.lockOwnedSlot(providerId, slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Availability slot not found."));
        if (slot.booked() || slot.slotDate().isBefore(LocalDate.now())) {
            throw new SlotUnavailableException();
        }
        if (providerRepository.hasAppointments(slotId)) {
            throw new SlotUnavailableException();
        }
        if (providerRepository.deleteUnbookedSlot(providerId, slotId) != 1) {
            throw new SlotUnavailableException();
        }
    }

    @Transactional
    public List<ProviderAppointmentDTO> getAppointments(String providerEmail) {
        providerRepository.completePastProviderAppointments(providerEmail);
        return providerRepository.findAppointments(providerEmail);
    }

    private int providerId(String email) {
        return providerRepository.findProviderIdByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Provider account not found."));
    }
}
