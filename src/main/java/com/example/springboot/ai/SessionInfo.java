package com.example.springboot.ai;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SessionInfo {
    private String sessionId;
    private String firstMessage;
    private LocalDateTime createTime;
}