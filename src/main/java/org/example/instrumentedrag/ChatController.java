package org.example.instrumentedrag;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class ChatController {
    private final ChatService chatService;
    private final ConversationSession session;

    public ChatController(ChatService chatService, ConversationSession session) {
        this.chatService = chatService;
        this.session = session;
    }

    @GetMapping("/chat")
    @ResponseStatus(HttpStatus.OK)
    public String chat(@RequestParam(value = "message") String message) {
        return this.chatService.respondToUserMessage(session, message).getResult().getOutput().getContent();
    }
}
