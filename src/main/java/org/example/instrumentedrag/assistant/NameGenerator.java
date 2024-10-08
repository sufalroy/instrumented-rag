package org.example.instrumentedrag.assistant;

public sealed interface NameGenerator permits RandomNameGenerator, MobyNameGenerator {
    String generateName();
}
