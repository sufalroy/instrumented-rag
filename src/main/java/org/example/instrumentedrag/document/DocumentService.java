package org.example.instrumentedrag.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.query.Criteria;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final VectorStore vectorStore;
    private final UploadedDocumentRepository uploadedDocumentRepository;
    private final ResourceLoader resourceLoader;
    private final ApplicationEventPublisher eventPublisher;
    private final CassandraTemplate cassandraTemplate;

    public DocumentService(VectorStore vectorStore,
                           UploadedDocumentRepository uploadedDocumentRepository,
                           ResourceLoader resourceLoader,
                           ApplicationEventPublisher eventPublisher, CassandraTemplate cassandraTemplate) {
        this.vectorStore = vectorStore;
        this.uploadedDocumentRepository = uploadedDocumentRepository;
        this.resourceLoader = resourceLoader;
        this.eventPublisher = eventPublisher;
        this.cassandraTemplate = cassandraTemplate;
    }

    @Transactional
    public UploadedDocument saveDocument(UploadedFileData uploadedFileData) {
        UploadedDocument savedUploadedDocument = this.uploadedDocumentRepository.save(uploadedFileData.toUploadedDocument());
        this.eventPublisher.publishEvent(new DocumentUploadedEvent(savedUploadedDocument));
        return savedUploadedDocument;
    }

    @Async
    @EventListener
    public void processUploadedDocumentEvent(DocumentUploadedEvent event) {
        UploadedDocument uploadedDocument = event.uploadedDocument();
        try {
            Resource resource = this.resourceLoader.getResource(uploadedDocument.getUrl());
            processUploadedDocument(uploadedDocument.getId().toString(), resource);
            updateDocumentStatus(uploadedDocument, DocumentStatus.READY);
        } catch (Exception e) {
            logger.error("Error processing document: {}", uploadedDocument.getId(), e);
            updateDocumentStatus(uploadedDocument, DocumentStatus.ERROR);
        }
    }

    private void processUploadedDocument(String uploadedDocumentId, Resource resource) {
        logger.info("Processing document: {} with ID: {}", resource.getFilename(), uploadedDocumentId);

        PdfDocumentReaderConfig config = createPdfReaderConfig();
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource, config);
        TokenTextSplitter textSplitter = new TokenTextSplitter();

        List<Document> documents = pdfReader.get();
        List<Document> splitDocuments = textSplitter.apply(documents);

        List<Document> documentsWithId = splitDocuments.stream()
                .map(doc -> appendDocumentId(doc, uploadedDocumentId))
                .collect(Collectors.toList());

        this.vectorStore.add(documentsWithId);
    }

    private PdfDocumentReaderConfig createPdfReaderConfig() {
        return PdfDocumentReaderConfig.builder()
                .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder()
                        .withNumberOfBottomTextLinesToDelete(0)
                        .withNumberOfTopPagesToSkipBeforeDelete(0)
                        .build())
                .withPagesPerDocument(1)
                .build();
    }

    private Document appendDocumentId(Document doc, String uploadedDocumentId) {
        Map<String, Object> newMetadata = new HashMap<>(doc.getMetadata());
        newMetadata.put("documentId", uploadedDocumentId);
        return new Document(doc.getContent(), newMetadata);
    }

    private void updateDocumentStatus(UploadedDocument uploadedDocument, DocumentStatus status) {
        uploadedDocument.setStatus(status);
        uploadedDocument.setUpdatedAt(Instant.now());
        this.uploadedDocumentRepository.save(uploadedDocument);
    }

    @Transactional(readOnly = true)
    public List<UploadedDocument> fetchUploadedDocuments(
            String name,
            Instant createdAfter,
            Instant createdBefore,
            String contentSearch
    ) {
        Query query = buildQuery(name, createdAfter, createdBefore, contentSearch);
        query = query.withAllowFiltering();
        return this.cassandraTemplate.select(query, UploadedDocument.class);
    }

    private Query buildQuery(@Nullable String name,
                             @Nullable Instant createdAfter,
                             @Nullable Instant createdBefore,
                             @Nullable String contentSearch) {
        Query query = Query.empty();

        if (name != null && !name.isBlank()) {
            query = query.and(Criteria.where("name").is(name));
        }
        if (createdAfter != null) {
            query = query.and(Criteria.where("createdAt").gte(createdAfter));
        }
        if (createdBefore != null) {
            query = query.and(Criteria.where("createdAt").lte(createdBefore));
        }

        if (contentSearch != null && !contentSearch.isBlank()) {
            Set<UUID> relevantIds = performContentSearch(contentSearch);
            query = query.and(Criteria.where("id").in(relevantIds));
        }

        return query;
    }

    private Set<UUID> performContentSearch(String searchQuery) {
        return this.vectorStore.similaritySearch(SearchRequest.query(searchQuery)
                        .withTopK(10)
                        .withSimilarityThreshold(0.7d))
                .stream()
                .filter(result -> result.getMetadata().containsKey("documentId"))
                .map(result -> UUID.fromString((String) result.getMetadata().get("documentId")))
                .collect(Collectors.toSet());
    }
}