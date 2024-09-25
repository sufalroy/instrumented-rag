package org.example.instrumentedrag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class ChatConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ChatConfiguration.class);

    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    @Primary
    @Bean
    public ChatModel primaryChatModel() {
        var openAiApi = new OpenAiApi("https://api.groq.com/openai", System.getenv("OPENAI_API_KEY"));
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
    OllamaChatModel secondaryChatModel(OllamaApi ollamaApi) {
        return new OllamaChatModel(ollamaApi,
                OllamaOptions.builder()
                        .withModel("gemma2:2b")
                        .build());
    }

    @Bean
    EmbeddingModel embeddingModel(OllamaApi ollamaApi) {
        return new OllamaEmbeddingModel(ollamaApi,
                OllamaOptions.builder()
                        .withModel("gemma2:2b")
                        .build());
    }

    @Bean
    public VectorStore vectorStore(
            EmbeddingModel embeddingModel,
            @Value("vector_store.json") String vectorStoreName,
            @Value("classpath:/docs/invoice.pdf") Resource document
    ) {
        SimpleVectorStore simpleVectorStore = new SimpleVectorStore(embeddingModel);

        Path path = Paths.get("src", "main", "resources", "data");
        File vectorStoreFile = path.resolve(vectorStoreName).toFile();

        if (vectorStoreFile.exists()) {
            logger.info("Loading vector store from {}", vectorStoreFile.getAbsolutePath());
            simpleVectorStore.load(vectorStoreFile);
        } else {
            logger.info("Vector store file {} does not exist, initializing new vector store", vectorStoreFile.getAbsolutePath());

            var config = PdfDocumentReaderConfig.builder()
                    .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder()
                            .withNumberOfBottomTextLinesToDelete(0)
                            .withNumberOfTopPagesToSkipBeforeDelete(0)
                            .build())
                    .withPagesPerDocument(1)
                    .build();

            var pdfReader = new PagePdfDocumentReader(document, config);
            var textSplitter = new TokenTextSplitter();

            var documents = pdfReader.get();
            var splitDocuments = textSplitter.apply(documents);

            simpleVectorStore.add(splitDocuments);
            simpleVectorStore.save(vectorStoreFile);
            logger.info("Saved new vector store to {}", vectorStoreFile.getAbsolutePath());
        }
        return simpleVectorStore;
    }

    @Bean
    NameGenerator nameGenerator() {
        return new MobyNameGenerator();
    }
}
