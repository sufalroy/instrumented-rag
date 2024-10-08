package org.example.instrumentedrag.assistant;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;
    private final ConversationSession session;

    public ChatController(ChatService chatService, ConversationSession session) {
        this.chatService = chatService;
        this.session = session;
    }

    @PostMapping("/{documentId}")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> chat(
            @Valid @RequestBody ConversationRequest request,
            @PathVariable String documentId
    ) {
        var response = chatService.respondToUserMessage(session, request.message(), documentId).getResult()
                .getOutput()
                .getContent();
        return Map.of("message", response);
    }
}
