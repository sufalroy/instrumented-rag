package org.example.instrumentedrag;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ChatConfiguration {

    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    @Primary
    @Bean
    public ChatModel primaryChatModel(@Value("${OPENAI_API_KEY}") String apiKey) {
        var openAiApi = new OpenAiApi("https://api.groq.com/openai", apiKey);
        return new OpenAiChatModel(openAiApi,
                OpenAiChatOptions.builder()
                        .withModel("llama3-70b-8192")
                        .withTemperature(0.7F)
                        .build());
    }

    @Bean
    OllamaApi ollamaApi() {
        return new OllamaApi("http://localhost:11434");
    }

    @Bean
    EmbeddingModel embeddingModel(OllamaApi ollamaApi) {
        return new OllamaEmbeddingModel(ollamaApi,
                OllamaOptions.builder()
                        .withModel("gemma2:2b")
                        .build());
    }

    @Bean
    NameGenerator nameGenerator() {
        return new MobyNameGenerator();
    }
}
