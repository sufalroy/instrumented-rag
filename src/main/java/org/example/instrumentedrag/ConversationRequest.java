package org.example.instrumentedrag;

import jakarta.validation.constraints.NotBlank;

public record ConversationRequest(
        @NotBlank(message = "Message cannot be empty")
        String message
) {
}
