package com.example.springboot.ai;

import lombok.Data;

@Data
public class ChatRequest {
    private String question;
    private String sessionId;
}