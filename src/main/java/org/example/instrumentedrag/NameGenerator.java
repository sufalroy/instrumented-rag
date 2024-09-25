package org.example.instrumentedrag;

public sealed interface NameGenerator permits RandomNameGenerator, MobyNameGenerator {
    String generateName();
}
