package com.cmpe172.fitness.service;

import com.cmpe172.fitness.dto.BookingConfirmationDTO;
import com.cmpe172.fitness.dto.MyAppointmentsDTO;
import com.cmpe172.fitness.exception.InvalidBookingRequestException;
import com.cmpe172.fitness.exception.ResourceNotFoundException;
import com.cmpe172.fitness.exception.SlotUnavailableException;
import com.cmpe172.fitness.repository.BookingRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    /**
     * READ_COMMITTED plus a row lock serializes attempts on this slot. The
     * database unique constraint on appointments.slot_id remains the backstop.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int bookSlot(int slotId, String customerEmail) {
        if (slotId <= 0) {
            throw new InvalidBookingRequestException();
        }

        int customerId = bookingRepository.findCustomerIdByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer account not found."));

        BookingRepository.LockedSlot slot = bookingRepository.lockSlotForBooking(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found."));
        if (slot.booked() || slot.slotDate().isBefore(LocalDate.now())) {
            throw new SlotUnavailableException();
        }

        if (bookingRepository.markSlotBooked(slotId) != 1) {
            throw new SlotUnavailableException();
        }

        try {
            return bookingRepository.insertAppointment(slotId, customerId);
        } catch (DuplicateKeyException exception) {
            throw new SlotUnavailableException();
        }
    }

    @Transactional(readOnly = true)
    public BookingConfirmationDTO getConfirmation(int appointmentId, String customerEmail) {
        if (appointmentId <= 0) {
            throw new InvalidBookingRequestException();
        }
        return bookingRepository.findConfirmation(appointmentId, customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));
    }

    @Transactional
    public MyAppointmentsDTO getMyAppointments(String customerEmail) {
        bookingRepository.completePastBookedAppointments(customerEmail);
        return new MyAppointmentsDTO(
                bookingRepository.findUpcomingAppointments(customerEmail),
                bookingRepository.findAppointmentHistory(customerEmail));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancelAppointment(int appointmentId, String customerEmail) {
        if (appointmentId <= 0) {
            throw new InvalidBookingRequestException();
        }

        bookingRepository.completePastBookedAppointments(customerEmail);
        BookingRepository.CancellableAppointment appointment =
                bookingRepository.lockAppointmentForCancellation(appointmentId, customerEmail)
                        .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));

        if (bookingRepository.markAppointmentCancelled(appointment.appointmentId()) != 1
                || bookingRepository.markSlotAvailable(appointment.slotId()) != 1) {
            throw new SlotUnavailableException();
        }
    }
}
