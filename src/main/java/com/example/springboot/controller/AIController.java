package com.example.springboot.controller;

import com.example.springboot.ai.*;
import com.example.springboot.common.Result;
import com.example.springboot.config.interceptor.AuthAccess;
import com.example.springboot.utils.TokenUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 助手控制器
 */
@RestController
@RequestMapping("/ai")
public class AIController {

    @Resource
    private AIService aiService;

    @Resource
    private KnowledgeSyncService knowledgeSyncService;

    /**
     * 非流式聊天
     * POST /ai/chat
     * Body: {"question": "...", "sessionId": "..."}
     */
    @PostMapping("/chat")
    public Result chat(@RequestBody ChatRequest request) {
        Integer userId = TokenUtils.getCurrentUser().getId();
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = aiService.createSession(userId);
        }
        ChatResponse response = aiService.chat(request.getQuestion(), sessionId, userId);
        return Result.success(response);
    }

    /**
     * SSE 流式聊天
     * GET /ai/chat/stream?question=xxx&sessionId=xxx
     */
    @GetMapping("/chat/stream")
    public SseEmitter streamChat(@RequestParam String question,
                                 @RequestParam(required = false) String sessionId,
                                 HttpServletResponse response) {
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        Integer userId = TokenUtils.getCurrentUser().getId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = aiService.createSession(userId);
        }
        return aiService.streamChat(question, sessionId, userId);
    }

    /**
     * 获取用户的所有会话列表
     * GET /ai/sessions
     */
    @GetMapping("/sessions")
    public Result getSessions() {
        Integer userId = TokenUtils.getCurrentUser().getId();
        return Result.success(aiService.getSessions(userId));
    }

    /**
     * 获取某个会话的消息历史
     * GET /ai/sessions/{sessionId}/messages
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public Result getSessionMessages(@PathVariable String sessionId) {
        Integer userId = TokenUtils.getCurrentUser().getId();
        return Result.success(aiService.getSessionMessages(sessionId, userId));
    }

    /**
     * 创建新会话
     * POST /ai/sessions
     */
    @PostMapping("/sessions")
    public Result createSession() {
        Integer userId = TokenUtils.getCurrentUser().getId();
        return Result.success(aiService.createSession(userId));
    }

    /**
     * 同步知识库（从所有博客重新生成嵌入）
     * POST /ai/knowledge/sync
     */
    @PostMapping("/knowledge/sync")
    @AuthAccess
    public Result syncKnowledge() {
        knowledgeSyncService.syncAll();
        return Result.success();
    }
}