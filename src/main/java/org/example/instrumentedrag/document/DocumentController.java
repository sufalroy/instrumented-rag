package org.example.instrumentedrag.document;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UploadedDocument saveDocument(@Valid @RequestBody UploadedFileData uploadedFileData) {
        return this.documentService.saveDocument(uploadedFileData);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<UploadedDocument> getAllDocuments() {
        return this.documentService.fetchAllUploadedDocuments();
    }
}