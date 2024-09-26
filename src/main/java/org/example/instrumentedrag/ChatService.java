package org.example.instrumentedrag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private final ChatModel primaryChatModel;
    private final VectorStore vectorStore;

    public ChatService(
            ChatModel primaryChatModel,
            VectorStore vectorStore
    ) {
        this.primaryChatModel = primaryChatModel;
        this.vectorStore = vectorStore;
    }

    public ChatResponse respondToUserMessage(ConversationSession conversationSession, String userMessage) {
        return chatClientForSession(conversationSession)
                .prompt()
                .advisors(advisorBuilder -> advisorBuilder.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationSession.getConversationId()))
                .advisors(advisorBuilder -> advisorBuilder.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, 50))
                .user(userMessage)
                .call()
                .chatResponse();
    }

    private ChatClient chatClientForSession(ConversationSession conversationSession) {
        return ChatClient
                .builder(this.primaryChatModel)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(conversationSession.getChatMemory()),
                        new QuestionAnswerAdvisor(
                                this.vectorStore,
                                SearchRequest.defaults().withSimilarityThreshold(0.3d)
                        ),
                        new SimpleLoggerAdvisor()
                )
                .defaultSystem(conversationSession.promptResource())
                .build();
    }
}
