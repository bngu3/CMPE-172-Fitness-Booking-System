package com.cmpe172.fitness.service;

import com.cmpe172.fitness.exception.InvalidBookingRequestException;
import com.cmpe172.fitness.exception.ResourceNotFoundException;
import com.cmpe172.fitness.exception.SlotUnavailableException;
import com.cmpe172.fitness.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void rejectsNonPositiveSlotId() {
        assertThrows(InvalidBookingRequestException.class,
                () -> bookingService.bookSlot(0, "alex.chen@example.com"));
        verify(bookingRepository, never()).lockSlotForBooking(0);
    }

    @Test
    void rejectsSlotAlreadyBooked() {
        when(bookingRepository.findCustomerIdByEmail("alex.chen@example.com"))
                .thenReturn(Optional.of(1));
        when(bookingRepository.lockSlotForBooking(7))
                .thenReturn(Optional.of(new BookingRepository.LockedSlot(7, LocalDate.now().plusDays(1), true)));

        assertThrows(SlotUnavailableException.class,
                () -> bookingService.bookSlot(7, "alex.chen@example.com"));
        verify(bookingRepository, never()).insertAppointment(7, 1);
    }

    @Test
    void booksAvailableSlotAndReturnsAppointmentId() {
        when(bookingRepository.findCustomerIdByEmail("alex.chen@example.com"))
                .thenReturn(Optional.of(1));
        when(bookingRepository.lockSlotForBooking(7))
                .thenReturn(Optional.of(new BookingRepository.LockedSlot(7, LocalDate.now().plusDays(1), false)));
        when(bookingRepository.markSlotBooked(7)).thenReturn(1);
        when(bookingRepository.insertAppointment(7, 1)).thenReturn(42);

        int appointmentId = bookingService.bookSlot(7, "alex.chen@example.com");

        assertEquals(42, appointmentId);
        verify(bookingRepository).markSlotBooked(7);
        verify(bookingRepository).insertAppointment(7, 1);
    }

    @Test
    void onlyOwnerCanCancelAppointment() {
        when(bookingRepository.lockAppointmentForCancellation(42, "jamie.rivera@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.cancelAppointment(42, "jamie.rivera@example.com"));
        verify(bookingRepository, never()).markAppointmentCancelled(42);
        verify(bookingRepository, never()).markSlotAvailable(7);
    }

    @Test
    void ownerCancellationMarksAppointmentCancelledAndFreesSlot() {
        when(bookingRepository.lockAppointmentForCancellation(42, "alex.chen@example.com"))
                .thenReturn(Optional.of(new BookingRepository.CancellableAppointment(42, 7)));
        when(bookingRepository.markAppointmentCancelled(42)).thenReturn(1);
        when(bookingRepository.markSlotAvailable(7)).thenReturn(1);

        bookingService.cancelAppointment(42, "alex.chen@example.com");

        verify(bookingRepository).markAppointmentCancelled(42);
        verify(bookingRepository).markSlotAvailable(7);
    }
}
