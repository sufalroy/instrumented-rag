package org.example.instrumentedrag;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.List;

@Component
@SessionScope
public class ConversationSession {
    private final ChatMemory chatMemory;
    private final String conversationId;
    private final Resource defaultSystemPrompt;

    public ConversationSession(
            ChatMemory chatMemory,
            NameGenerator nameGenerator,
            @Value("classpath:/prompts/system-prompt.st") Resource defaultSystemPrompt) {
        this.chatMemory = chatMemory;
        this.conversationId = nameGenerator.generateName();
        this.defaultSystemPrompt = defaultSystemPrompt;
    }

    public List<Message> messages() {
        return this.chatMemory.get(conversationId, 100);
    }

    public Resource promptResource() {
        return this.defaultSystemPrompt;
    }

    public String getConversationId() {
        return this.conversationId;
    }

    public ChatMemory getChatMemory() {
        return this.chatMemory;
    }
}