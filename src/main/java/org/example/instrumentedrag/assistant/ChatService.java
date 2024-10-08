package org.example.instrumentedrag.assistant;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;


@Service
public class ChatService {
    private final ChatModel primaryChatModel;
    private final OllamaChatModel localChatModel;
    private final VectorStore vectorStore;
    private final Executor executor;

    public ChatService(
            ChatModel primaryChatModel,
            OllamaChatModel localChatModel,
            VectorStore vectorStore,
            Executor executor
    ) {
        this.primaryChatModel = primaryChatModel;
        this.localChatModel = localChatModel;
        this.vectorStore = vectorStore;
        this.executor = executor;
    }

    public ChatResponse respondToUserMessage(
            ConversationSession conversationSession,
            String userMessage,
            String documentId
    ) {
        return chatClientForSession(conversationSession, documentId)
                .prompt()
                .advisors(advisorBuilder -> advisorBuilder.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationSession.getConversationId()))
                .advisors(advisorBuilder -> advisorBuilder.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, 50))
                .user(userMessage)
                .call()
                .chatResponse();
    }

    private ChatClient chatClientForSession(ConversationSession conversationSession, String documentId) {
        return ChatClient
                .builder(this.primaryChatModel)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(conversationSession.getChatMemory()),
                        new CaptureMemoryAdvisor(
                                this.vectorStore,
                                this.localChatModel,
                                this.executor
                        ),
                        new QuestionAnswerAdvisor(
                                this.vectorStore,
                                SearchRequest.defaults()
                                        .withTopK(3)
                                        .withFilterExpression(new FilterExpressionBuilder()
                                                .in("documentId", documentId)
                                                .build())
                        ),
                        new SimpleLoggerAdvisor()
                )
                .defaultSystem(conversationSession.promptResource())
                .build();
    }
}

