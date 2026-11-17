package com.cmpe172.fitness.agent;

import com.cmpe172.fitness.dto.AppointmentDTO;
import com.cmpe172.fitness.dto.MyAppointmentsDTO;
import com.cmpe172.fitness.dto.SlotDTO;
import com.cmpe172.fitness.service.BookingService;
import com.cmpe172.fitness.service.SlotService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Request-scoped tools. Every user identity is fixed by the authenticated controller request. */
public class FitnessBookingTools {
    private final String customerEmail;
    private final String conversationId;
    private final SlotService slotService;
    private final BookingService bookingService;
    private final PendingAgentActionStore pendingActions;

    public FitnessBookingTools(String customerEmail, String conversationId, SlotService slotService,
                               BookingService bookingService, PendingAgentActionStore pendingActions) {
        this.customerEmail = customerEmail;
        this.conversationId = conversationId;
        this.slotService = slotService;
        this.bookingService = bookingService;
        this.pendingActions = pendingActions;
    }

    @Tool(description = "Search bookable fitness sessions between two dates (inclusive). Dates use YYYY-MM-DD. Leave dates blank to search today through the next seven days. Filter by a service name when requested.")
    public String checkAvailability(
            @ToolParam(description = "First date, YYYY-MM-DD, or blank") String fromDate,
            @ToolParam(description = "Last date, YYYY-MM-DD, or blank") String toDate,
            @ToolParam(description = "Optional service name or blank") String serviceName) {
        LocalDate from;
        LocalDate to;
        try {
            from = fromDate == null || fromDate.isBlank() ? LocalDate.now() : LocalDate.parse(fromDate.trim());
            to = toDate == null || toDate.isBlank() ? from.plusDays(7) : LocalDate.parse(toDate.trim());
        } catch (DateTimeParseException exception) {
            return "Please provide dates in YYYY-MM-DD format.";
        }
        if (to.isBefore(from) || to.isAfter(from.plusDays(13))) {
            return "Search ranges must be between one and fourteen days.";
        }

        List<SlotDTO> slots = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            slots.addAll(slotService.getAvailableSlots(null, null, day, 100, 0));
        }
        if (serviceName != null && !serviceName.isBlank()) {
            String requestedService = serviceName.toLowerCase(Locale.ROOT);
            slots.removeIf(slot -> !slot.getServiceName().toLowerCase(Locale.ROOT).contains(requestedService));
        }
        if (slots.isEmpty()) return "No matching bookable sessions were found in that date range.";
        return slots.stream().limit(30).map(FitnessBookingTools::formatSlot)
                .reduce((left, right) -> left + "\n" + right).orElse("No matching sessions.");
    }

    @Tool(description = "List the signed-in customer's upcoming appointments and appointment history.")
    public String listMyAppointments() {
        MyAppointmentsDTO appointments = bookingService.getMyAppointments(customerEmail);
        List<String> lines = new ArrayList<>();
        appointments.upcoming().forEach(appointment -> lines.add(formatAppointment(appointment)));
        appointments.history().forEach(appointment -> lines.add(formatAppointment(appointment)));
        return lines.isEmpty() ? "You have no appointments." : String.join("\n", lines);
    }

    @Tool(description = "Prepare a booking request for a currently available session. This only proposes the booking and never commits it; the customer must confirm in a later message.")
    public String requestBooking(@ToolParam(description = "The session slot ID") int slotId) {
        var slot = slotService.findAvailableSlot(slotId);
        if (slot.isEmpty()) {
            return "That session is no longer available. Search availability again and offer the closest alternatives; do not claim it was booked.";
        }
        SlotDTO selected = slot.get();
        String summary = formatSlot(selected);
        pendingActions.put(conversationId,
                new PendingAgentAction(PendingAgentAction.Type.BOOKING, slotId, summary));
        return "Booking proposal only; not booked. Ask the customer to confirm this exact session before committing: " + summary;
    }

    @Tool(description = "Prepare cancellation of one of the signed-in customer's future appointments. This only proposes cancellation and never commits it; the customer must confirm in a later message.")
    public String requestCancellation(@ToolParam(description = "The appointment ID") int appointmentId) {
        MyAppointmentsDTO appointments = bookingService.getMyAppointments(customerEmail);
        AppointmentDTO appointment = appointments.upcoming().stream()
                .filter(item -> item.appointmentId() == appointmentId)
                .findFirst().orElse(null);
        if (appointment == null) return "That appointment is not a cancellable upcoming appointment on your account.";
        String summary = formatAppointment(appointment);
        pendingActions.put(conversationId,
                new PendingAgentAction(PendingAgentAction.Type.CANCELLATION, appointmentId, summary));
        return "Cancellation proposal only; not cancelled. Ask the customer to confirm this appointment before committing: " + summary;
    }

    private static String formatSlot(SlotDTO slot) {
        return "Slot " + slot.getSlotId() + ": " + slot.getServiceName() + " with " + slot.getProviderName()
                + " on " + slot.getSlotDate() + " from " + slot.getStartTime() + " to " + slot.getEndTime()
                + " ($" + String.format(Locale.ROOT, "%.2f", slot.getPrice()) + ")";
    }

    private static String formatAppointment(AppointmentDTO appointment) {
        return "Appointment " + appointment.appointmentId() + ": " + appointment.serviceName() + " with "
                + appointment.providerName() + " on " + appointment.slotDate() + " from "
                + appointment.startTime() + " to " + appointment.endTime() + " (" + appointment.status() + ")";
    }
}
