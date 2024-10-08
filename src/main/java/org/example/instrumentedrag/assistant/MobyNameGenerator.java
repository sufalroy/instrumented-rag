package org.example.instrumentedrag.assistant;

public record MobyNameGenerator() implements NameGenerator {
    @Override
    public String generateName() {
        return info.schnatterer.mobynamesgenerator.MobyNamesGenerator.getRandomName();
    }
}
