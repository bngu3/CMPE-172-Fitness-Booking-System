package com.cmpe172.fitness.agent;

import com.cmpe172.fitness.dto.AgentChatResponse;
import com.cmpe172.fitness.dto.NotificationRequest;
import com.cmpe172.fitness.service.BookingService;
import com.cmpe172.fitness.service.NotificationClient;
import com.cmpe172.fitness.service.SlotService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
public class FitnessAgentService {
    private static final String SYSTEM_PROMPT = """
            You are the fitness studio's logged-in customer's booking assistant.
            Use tools for live availability and account appointment facts; never invent them.
            You may search multiple dates and compare results to find the earliest matching session.
            Booking and cancellation tools only prepare proposals. They never complete a write.
            Clearly summarize the exact appointment and ask the customer to confirm in a separate turn.
            If a requested slot is unavailable, say so, search for nearby alternatives, and offer them.
            Never tell the user a booking or cancellation is complete unless the application confirms it.
            The authenticated customer identity is fixed by the application; never ask for passwords or email.
            """;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final BookingService bookingService;
    private final SlotService slotService;
    private final PendingAgentActionStore pendingActions;
    private final NotificationClient notificationClient;

    public FitnessAgentService(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory,
                               BookingService bookingService, SlotService slotService,
                               PendingAgentActionStore pendingActions, NotificationClient notificationClient) {
        this.chatClient = chatClientBuilder.build();
        this.chatMemory = chatMemory;
        this.bookingService = bookingService;
        this.slotService = slotService;
        this.pendingActions = pendingActions;
        this.notificationClient = notificationClient;
    }

    public AgentChatResponse chat(String customerEmail, String sessionId, String message) {
        String conversationId = conversationId(customerEmail, sessionId);
        PendingAgentAction pending = pendingActions.get(conversationId);
        if (pending != null) {
            if (isConfirmation(message)) {
                pendingActions.remove(conversationId);
                return new AgentChatResponse(conversationId, commit(customerEmail, conversationId, message, pending));
            }
            if (isDecline(message)) {
                pendingActions.remove(conversationId);
            } else {
                return new AgentChatResponse(conversationId,
                        "I have this pending request: " + pending.summary()
                                + ". Reply 'yes' to confirm it, or 'no' to discard it.");
            }
        }

        FitnessBookingTools tools = new FitnessBookingTools(customerEmail, conversationId,
                slotService, bookingService, pendingActions);
        String reply = chatClient.prompt()
                .system(SYSTEM_PROMPT + " Today's date is " + LocalDate.now() + ".")
                .user(message)
                .tools(tools)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
        return new AgentChatResponse(conversationId, reply == null ? "I couldn't create a response. Please try again." : reply);
    }

    public Flux<String> streamChat(String customerEmail, String sessionId, String message) {
        String conversationId = conversationId(customerEmail, sessionId);
        PendingAgentAction pending = pendingActions.get(conversationId);
        if (pending != null) {
            if (isConfirmation(message)) {
                pendingActions.remove(conversationId);
                return Mono.fromCallable(() -> commit(customerEmail, conversationId, message, pending)).flux();
            }
            if (isDecline(message)) {
                pendingActions.remove(conversationId);
            } else {
                return Flux.just("I have this pending request: " + pending.summary()
                        + ". Reply 'yes' to confirm it, or 'no' to discard it.");
            }
        }
        FitnessBookingTools tools = new FitnessBookingTools(customerEmail, conversationId,
                slotService, bookingService, pendingActions);
        return chatClient.prompt()
                .system(SYSTEM_PROMPT + " Today's date is " + LocalDate.now() + ".")
                .user(message)
                .tools(tools)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    private static String conversationId(String customerEmail, String sessionId) {
        return UUID.nameUUIDFromBytes((customerEmail.toLowerCase(Locale.ROOT) + ":" + sessionId)
                .getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String commit(String customerEmail, String conversationId, String userMessage, PendingAgentAction pending) {
        String result;
        if (pending.type() == PendingAgentAction.Type.BOOKING) {
            int appointmentId = bookingService.bookSlot(pending.targetId(), customerEmail);
            var confirmation = bookingService.getConfirmation(appointmentId, customerEmail);
            notificationClient.send(new NotificationRequest("appointment-" + appointmentId + ":BOOKED",
                    customerEmail, "BOOKING_CONFIRMED", "Your " + confirmation.serviceName() + " session is booked for "
                    + confirmation.slotDate() + " at " + confirmation.startTime() + "."));
            result = "Booking confirmed. Appointment " + appointmentId + " is booked: " + pending.summary() + ".";
        } else {
            bookingService.cancelAppointment(pending.targetId(), customerEmail);
            notificationClient.send(new NotificationRequest("appointment-" + pending.targetId() + ":CANCELLED",
                    customerEmail, "BOOKING_CANCELLED", "Your appointment " + pending.targetId() + " was cancelled."));
            result = "Cancellation confirmed. " + pending.summary() + " has been cancelled.";
        }
        try {
            String response = chatClient.prompt()
                    .system("The application completed this confirmed action: " + result
                            + " State the result clearly and do not claim anything beyond it.")
                    .user(userMessage)
                    .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                    .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call().content();
            return response == null || response.isBlank() ? result : response;
        } catch (RuntimeException ignored) {
            return result;
        }
    }

    private static boolean isConfirmation(String message) {
        String normalized = message.trim().toLowerCase(Locale.ROOT).replaceAll("[.!]$", "");
        return normalized.matches("(yes|yeah|yep|confirm|confirmed|book it|cancel it|go ahead|proceed|yes,? (please|confirm|book it|cancel it))");
    }

    private static boolean isDecline(String message) {
        String normalized = message.trim().toLowerCase(Locale.ROOT).replaceAll("[.!]$", "");
        return normalized.matches("(no|no thanks|no thank you|don't|do not|stop|never mind|nevermind|reject)");
    }
}
