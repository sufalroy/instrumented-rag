package org.example.instrumentedrag.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final VectorStore vectorStore;
    private final UploadedDocumentRepository uploadedDocumentRepository;
    private final ResourceLoader resourceLoader;
    private final ApplicationEventPublisher eventPublisher;

    public DocumentService(VectorStore vectorStore,
                           UploadedDocumentRepository uploadedDocumentRepository,
                           ResourceLoader resourceLoader,
                           ApplicationEventPublisher eventPublisher) {
        this.vectorStore = vectorStore;
        this.uploadedDocumentRepository = uploadedDocumentRepository;
        this.resourceLoader = resourceLoader;
        this.eventPublisher = eventPublisher;
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

    public List<UploadedDocument> fetchAllUploadedDocuments() {
        return this.uploadedDocumentRepository.findAll();
    }
}