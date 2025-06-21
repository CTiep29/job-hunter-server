package vn.ctiep.jobhunter.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatResponse {
    private String message;
    private LocalDateTime timestamp;

    public ChatResponse(String message, LocalDateTime timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }
}