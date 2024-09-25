package org.example.instrumentedrag;

public record MobyNameGenerator() implements NameGenerator {
    @Override
    public String generateName() {
        return info.schnatterer.mobynamesgenerator.MobyNamesGenerator.getRandomName();
    }
}
