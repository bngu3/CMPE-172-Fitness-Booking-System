package com.cmpe172.fitness.agent;

import com.cmpe172.fitness.exception.SlotUnavailableException;
import com.cmpe172.fitness.repository.BookingRepository;
import com.cmpe172.fitness.service.BookingService;
import com.cmpe172.fitness.service.NotificationClient;
import com.cmpe172.fitness.service.SlotService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FitnessAgentBookingTest {
    private static final String CUSTOMER_EMAIL = "alex.chen@example.com";
    private static final String SESSION_ID = "agent-booking-test";
    private static final int SLOT_ID = 7;

    @Test
    void confirmedAgentBookingUsesTransactionalServiceAndRejectsAlreadyBookedSlot() throws Exception {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        BookingService bookingService = new BookingService(bookingRepository);
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        when(chatClientBuilder.build()).thenReturn(mock(ChatClient.class));
        PendingAgentActionStore pendingActions = new PendingAgentActionStore();
        String conversationId = UUID.nameUUIDFromBytes(
                (CUSTOMER_EMAIL + ":" + SESSION_ID).getBytes(StandardCharsets.UTF_8)).toString();
        pendingActions.put(conversationId, new PendingAgentAction(
                PendingAgentAction.Type.BOOKING, SLOT_ID, "Slot 7: Yoga"));

        when(bookingRepository.findCustomerIdByEmail(CUSTOMER_EMAIL)).thenReturn(Optional.of(1));
        when(bookingRepository.lockSlotForBooking(SLOT_ID)).thenReturn(Optional.of(
                new BookingRepository.LockedSlot(SLOT_ID, LocalDate.now().plusDays(1), LocalTime.NOON, true)));

        FitnessAgentService agent = new FitnessAgentService(chatClientBuilder, mock(ChatMemory.class),
                bookingService, mock(SlotService.class), pendingActions, mock(NotificationClient.class));

        assertThrows(SlotUnavailableException.class, () -> agent.chat(CUSTOMER_EMAIL, SESSION_ID, "yes"));

        verify(bookingRepository).lockSlotForBooking(SLOT_ID);
        verify(bookingRepository, never()).markSlotBooked(SLOT_ID);
        verify(bookingRepository, never()).insertAppointment(SLOT_ID, 1);
        Method bookingMethod = BookingService.class.getMethod("bookSlot", int.class, String.class);
        assertNotNull(bookingMethod.getAnnotation(Transactional.class),
                "Agent booking must use the transactional BookingService method");
    }
}
