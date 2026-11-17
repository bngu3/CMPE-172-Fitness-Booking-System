package com.cmpe172.fitness.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class FitnessPolicyQaService {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final Resource knowledgeSource;
    private volatile boolean ingested;

    public FitnessPolicyQaService(ChatClient.Builder builder, EmbeddingModel embeddingModel,
                                  ResourceLoader resourceLoader) {
        this.chatClient = builder.build();
        this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        this.knowledgeSource = resourceLoader.getResource("classpath:knowledge/fitness-studio-faq.md");
    }

    public synchronized String ask(String question) {
        if (!ingested) ingestKnowledge();
        List<Document> matches = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(4).build());
        if (matches == null || matches.isEmpty()) {
            return "I couldn't find that in the studio's policy information. Please contact the studio for confirmation.";
        }
        String context = matches.stream().map(Document::getText).reduce((a, b) -> a + "\n\n" + b).orElse("");
        String reply = chatClient.prompt()
                .system("Answer only from the supplied studio policy excerpts. If the answer is absent, say the policy source does not specify and do not guess. Distinguish written policy from live appointment availability.")
                .user("Policy excerpts:\n" + context + "\n\nQuestion: " + question)
                .call().content();
        return reply == null ? "I couldn't generate an answer. Please try again." : reply;
    }

    private void ingestKnowledge() {
        try (var input = knowledgeSource.getInputStream()) {
            String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            vectorStore.add(List.of(new Document(text, Map.of("source", "fitness-studio-faq.md"))));
            ingested = true;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not ingest the fitness studio policy source.", exception);
        }
    }
}
