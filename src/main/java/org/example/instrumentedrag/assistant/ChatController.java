package org.example.instrumentedrag.assistant;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/assistant")
public class ChatController {
    private final ChatService chatService;
    private final ConversationSession session;

    public ChatController(ChatService chatService, ConversationSession session) {
        this.chatService = chatService;
        this.session = session;
    }

    @GetMapping("/{documentId}")
    @ResponseStatus(HttpStatus.OK)
    public Flux<String> chat(
            @PathVariable String documentId,
            @RequestParam(defaultValue = "please give a summary of the document") String message

    ) {
        return this.chatService.respondToUserMessage(session, message, documentId);
    }
}
