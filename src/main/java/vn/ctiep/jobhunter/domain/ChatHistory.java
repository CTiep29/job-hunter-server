package vn.ctiep.jobhunter.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "chat_history")
public class ChatHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String question;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String answer;
    
    private LocalDateTime timestamp;
}
