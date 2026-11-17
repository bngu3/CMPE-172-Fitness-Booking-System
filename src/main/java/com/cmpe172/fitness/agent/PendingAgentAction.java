package com.cmpe172.fitness.agent;

public record PendingAgentAction(Type type, int targetId, String summary) {
    public enum Type { BOOKING, CANCELLATION }
}
