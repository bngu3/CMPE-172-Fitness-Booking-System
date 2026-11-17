package com.cmpe172.fitness.controller;

import com.cmpe172.fitness.agent.FitnessAgentService;
import com.cmpe172.fitness.dto.AgentChatRequest;
import com.cmpe172.fitness.dto.AgentChatResponse;
import com.cmpe172.fitness.dto.ReminderRunResult;
import com.cmpe172.fitness.service.AppointmentReminderService;
import com.cmpe172.fitness.service.FitnessPolicyQaService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;

@RestController
@Validated
@RequestMapping("/api/agent")
public class FitnessAgentController {
    private final FitnessAgentService fitnessAgentService;
    private final FitnessPolicyQaService policyQaService;
    private final AppointmentReminderService reminderService;

    public FitnessAgentController(FitnessAgentService fitnessAgentService,
                                  FitnessPolicyQaService policyQaService,
                                  AppointmentReminderService reminderService) {
        this.fitnessAgentService = fitnessAgentService;
        this.policyQaService = policyQaService;
        this.reminderService = reminderService;
    }

    @PostMapping("/chat")
    public AgentChatResponse chat(@Valid @RequestBody AgentChatRequest request,
                                  Authentication authentication, HttpSession session) {
        return fitnessAgentService.chat(authentication.getName(), session.getId(), request.message());
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@Valid @RequestBody AgentChatRequest request,
                                  Authentication authentication, HttpSession session) {
        return fitnessAgentService.streamChat(authentication.getName(), session.getId(), request.message());
    }

    @PostMapping("/ask")
    public String askPolicyQuestion(@Valid @RequestBody AgentChatRequest request) {
        return policyQaService.ask(request.message());
    }

    @PostMapping("/reminders/run")
    public ReminderRunResult runReminders(
            @RequestParam(defaultValue = "7") @Min(1) @Max(30) int lookAheadDays) {
        return reminderService.run(lookAheadDays);
    }
}
