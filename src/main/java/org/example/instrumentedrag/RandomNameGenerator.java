package org.example.instrumentedrag;

import java.util.UUID;

public record RandomNameGenerator() implements NameGenerator {
    @Override
    public String generateName() {
        return UUID.randomUUID().toString();
    }
}
