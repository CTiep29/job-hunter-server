package vn.ctiep.jobhunter.domain;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private String userId;
}
