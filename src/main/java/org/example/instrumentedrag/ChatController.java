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

    @PostMapping("/chat")
    @ResponseStatus(HttpStatus.OK)
    public String chat(@RequestParam(value = "message") String message) {
        return chatService.respondToUserMessage(session, message).getResult().getOutput().getContent();
    }
}
