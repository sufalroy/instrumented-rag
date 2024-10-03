package org.example.instrumentedrag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.AdvisedRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.RequestResponseAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class CaptureMemoryAdvisor implements RequestResponseAdvisor {

    private static final Logger logger = LoggerFactory.getLogger(CaptureMemoryAdvisor.class);

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final Executor executor;
    private final MemoryBasisExtractor memoryBasisExtractor;
    private final RetryTemplate retryTemplate;
    private final ObjectMapper objectMapper;

    public CaptureMemoryAdvisor(VectorStore vectorStore,
                                ChatModel chatModel,
                                Executor executor
    ) {
        this(vectorStore, chatModel, executor,
                new RetryTemplateBuilder().maxAttempts(3).fixedBackoff(1000).build());
    }

    public CaptureMemoryAdvisor(VectorStore vectorStore,
                                ChatModel chatModel,
                                Executor executor,
                                RetryTemplate retryTemplate) {
        this.vectorStore = vectorStore;
        this.executor = executor;
        this.memoryBasisExtractor = lastMessageMemoryBasisExtractor();
        this.retryTemplate = retryTemplate;

        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(new ClassPathResource("prompts/capture_memory.st"))
                .build();

        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public AdvisedRequest adviseRequest(AdvisedRequest request, Map<String, Object> context) {
        executor.execute(() -> {
            try {
                retryTemplate.execute((context1) -> extractMemoryIfPossible(request));
            } catch (Throwable t) {
                logger.error("Failed to extract memory after multiple attempts", t);
            }
        });
        return request;
    }

    private boolean extractMemoryIfPossible(AdvisedRequest request) {
        var memoryLlmResponse = chatClient.prompt()
                .messages(memoryBasisExtractor.apply(request))
                .call()
                .entity(new BeanOutputConverter<>(MemoryLlmResponse.class, objectMapper));

        if (memoryLlmResponse.worthKeeping()) {
            logger.info("Adding memory: {}", memoryLlmResponse);
            vectorStore.add(List.of(new Document(
                    "Remember this about the user:\n" + memoryLlmResponse.content()
            )));
            return true;
        }

        logger.info("Ignoring useless potential memory: {}", memoryLlmResponse);
        return false;
    }

    public record MemoryLlmResponse(String content, boolean useful) {
        public boolean worthKeeping() {
            return useful && content != null && !content.isBlank();
        }
    }

    public static MemoryBasisExtractor lastMessageMemoryBasisExtractor() {
        return request -> List.of(new UserMessage(request.userText()));
    }
}

