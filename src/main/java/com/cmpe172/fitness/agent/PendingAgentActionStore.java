package com.cmpe172.fitness.agent;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class PendingAgentActionStore {
    private final ConcurrentHashMap<String, PendingAgentAction> pending = new ConcurrentHashMap<>();

    public void put(String conversationId, PendingAgentAction action) {
        pending.put(conversationId, action);
    }

    public PendingAgentAction get(String conversationId) {
        return pending.get(conversationId);
    }

    public void remove(String conversationId) {
        pending.remove(conversationId);
    }
}
