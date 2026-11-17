package com.cmpe172.fitness.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentChatRequest(@NotBlank String message) { }
