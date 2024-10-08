package org.example.instrumentedrag.assistant;

import org.springframework.ai.chat.client.AdvisedRequest;
import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.function.Function;

public interface MemoryBasisExtractor extends Function<AdvisedRequest, List<Message>> {
}
