package org.example.instrumentedrag.document;

import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.UUID;

public interface UploadedDocumentRepository extends CassandraRepository<UploadedDocument, UUID> {
}
