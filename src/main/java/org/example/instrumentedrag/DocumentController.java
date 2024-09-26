package org.example.instrumentedrag;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PutMapping("/upload")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        return this.documentService.processDocument(file);
    }
}
