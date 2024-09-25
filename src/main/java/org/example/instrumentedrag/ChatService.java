package org.example.instrumentedrag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {
    private final ChatModel primaryChatModel;
    private final VectorStore vectorStore;

    @Value("classpath:/prompts/rag-promt-template.st")
    private Resource ragPromptTemplate;

    public ChatService(
            ChatModel primaryChatModel,
            VectorStore vectorStore
    ) {
        this.primaryChatModel = primaryChatModel;
        this.vectorStore = vectorStore;
    }

    public ChatResponse respondToUserMessage(ConversationSession conversationSession, String userMessage) {
        PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplate);
        Map<String, Object> promptParameters = new HashMap<>();
        promptParameters.put("input", userMessage);
        promptParameters.put("documents", String.join("\n", findSimilarDocuments(userMessage)));
        Prompt prompt = promptTemplate.create(promptParameters);

        return chatClientForSession(conversationSession)
                .prompt()
                .advisors(advisorBuilder -> advisorBuilder.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationSession.getConversationId()))
                .advisors(advisorBuilder -> advisorBuilder.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, 50))
                .user(prompt.getContents())
                .call()
                .chatResponse();
    }

    private List<String> findSimilarDocuments(String message) {
        return vectorStore.similaritySearch(SearchRequest.query(message).withTopK(3))
                .stream()
                .map(Document::getContent)
                .toList();
    }

    private ChatClient chatClientForSession(ConversationSession conversationSession) {
        return ChatClient
                .builder(primaryChatModel)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(conversationSession.getChatMemory()),
                        new QuestionAnswerAdvisor(
                                vectorStore,
                                SearchRequest.defaults().withSimilarityThreshold(0.8)
                        ),
                        new SimpleLoggerAdvisor()
                )
                .defaultSystem(conversationSession.promptResource())
                .build();
    }
}
