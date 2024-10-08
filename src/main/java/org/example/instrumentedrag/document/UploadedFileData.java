package org.example.instrumentedrag.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record UploadedFileData(
        @NotBlank String name,
        @NotNull long size,
        @NotBlank String type,
        @NotBlank String key,
        @NotBlank String url,
        @NotBlank String appUrl
) {
    public UploadedDocument toUploadedDocument() {
        return UploadedDocument.builder()
                .id(UUID.randomUUID())
                .name(this.name)
                .size(this.size)
                .type(this.type)
                .key(this.key)
                .url(this.url)
                .appUrl(this.appUrl)
                .status(DocumentStatus.PROCESSING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
