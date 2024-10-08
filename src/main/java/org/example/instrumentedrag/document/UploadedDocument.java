package org.example.instrumentedrag.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(value = "uploaded_documents")
public class UploadedDocument {

    @Id
    @PrimaryKeyColumn(name = "id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    @CassandraType(type = CassandraType.Name.UUID)
    private UUID id;

    @CassandraType(type = CassandraType.Name.TEXT)
    private String name;

    @CassandraType(type = CassandraType.Name.BIGINT)
    private long size;

    @CassandraType(type = CassandraType.Name.TEXT)
    private String type;

    @CassandraType(type = CassandraType.Name.TEXT)
    private String key;

    @CassandraType(type = CassandraType.Name.TEXT)
    private String url;

    @Column("app_url")
    @CassandraType(type = CassandraType.Name.TEXT)
    private String appUrl;

    @CassandraType(type = CassandraType.Name.VARCHAR)
    private DocumentStatus status;

    @Column("created_at")
    @CassandraType(type = CassandraType.Name.TIMESTAMP)
    private Instant createdAt;

    @Column("updated_at")
    @CassandraType(type = CassandraType.Name.TIMESTAMP)
    private Instant updatedAt;
}
