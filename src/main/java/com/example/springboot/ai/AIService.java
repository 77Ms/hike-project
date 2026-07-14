package com.example.springboot.ai;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springboot.entity.ChatMessage;
import com.example.springboot.entity.KnowledgeEmbedding;
import com.example.springboot.mapper.ChatMessageMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AIService {

    @Resource
    private AiConfig aiConfig;

    @Resource
    private LlmClient llmClient;

    @Resource
    private EmbeddingService embeddingService;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    // ========== 非流式聊天 ==========

    /**
     * 非流式 RAG 聊天
     *
     * @param question  用户问题
     * @param sessionId 会话 ID
     * @param userId    用户 ID
     * @return 聊天响应
     */
    public ChatResponse chat(String question, String sessionId, Integer userId) {
        // 1. 保存用户消息
        saveMessage(userId, sessionId, "user", question);

        // 2. 语义搜索相关文档
        List<KnowledgeEmbedding> relevantDocs = embeddingService.semanticSearch(question);

        // 3. 构建 prompt
        JSONArray messages = buildPrompt(question, relevantDocs, sessionId, userId);

        // 4. 调用 LLM
        String answer = llmClient.chatCompletion(messages);

        // 5. 保存助手回答
        saveMessage(userId, sessionId, "assistant", answer);

        return new ChatResponse(answer, sessionId);
    }

    // ========== 流式聊天 (SSE) ==========

    /**
     * 流式 RAG 聊天，返回 SseEmitter
     *
     * @param question  用户问题
     * @param sessionId 会话 ID
     * @param userId    用户 ID
     * @return SseEmitter
     */
    public SseEmitter streamChat(String question, String sessionId, Integer userId) {
        // 1. 保存用户消息
        saveMessage(userId, sessionId, "user", question);

        // 2. 语义搜索
        List<KnowledgeEmbedding> relevantDocs = embeddingService.semanticSearch(question);

        // 3. 构建 prompt
        JSONArray messages = buildPrompt(question, relevantDocs, sessionId, userId);

        // 4. 创建 SseEmitter（超时 5 分钟）
        SseEmitter emitter = new SseEmitter(300000L);

        // 5. 在独立线程中流式读取 LLM 响应
        new Thread(() -> {
            try (Response response = llmClient.streamingChatCompletion(messages);
                 ResponseBody body = response.body()) {

                if (body == null) {
                    emitter.send(SseEmitter.event().name("error").data("Empty response body"));
                    emitter.complete();
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(body.byteStream(), "UTF-8"));
                String line;
                StringBuilder fullContent = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            emitter.send(SseEmitter.event().name("done").data(""));
                            break;
                        }
                        // 解析 delta 内容
                        String delta = parseDeltaContent(data);
                        if (delta != null) {
                            fullContent.append(delta);
                            emitter.send(SseEmitter.event().name("message").data(delta, new MediaType("text", "plain", StandardCharsets.UTF_8)));
                        }
                    }
                }

                // 保存完整 AI 回答
                saveMessage(userId, sessionId, "assistant", fullContent.toString());
                emitter.complete();

            } catch (Exception e) {
                log.error("SSE streaming error", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("服务异常: " + e.getMessage()));
                } catch (Exception ex) {
                    log.error("发送错误消息失败", ex);
                }
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    // ========== 会话管理 ==========

    /**
     * 创建新会话
     *
     * @param userId 用户 ID
     * @return 新会话 ID
     */
    public String createSession(Integer userId) {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取用户的所有会话列表
     *
     * @param userId 用户 ID
     * @return 会话列表
     */
    public List<SessionInfo> getSessions(Integer userId) {
        // 使用 MyBatis-Plus 查询分组后的会话信息
        // 由于 MyBatis-Plus 的 BaseMapper 不支持 GROUP BY，我们手动处理
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getUserId, userId)
                .orderByDesc(ChatMessage::getCreateTime);

        List<ChatMessage> allMessages = chatMessageMapper.selectList(wrapper);

        // 按 sessionId 分组
        List<SessionInfo> sessions = new ArrayList<>();
        String lastSessionId = null;

        for (ChatMessage msg : allMessages) {
            if (!msg.getSessionId().equals(lastSessionId)) {
                lastSessionId = msg.getSessionId();
                SessionInfo info = new SessionInfo();
                info.setSessionId(msg.getSessionId());
                info.setFirstMessage(msg.getContent());
                info.setCreateTime(msg.getCreateTime());
                sessions.add(info);
            }
        }

        return sessions;
    }

    /**
     * 获取某个会话的所有消息
     *
     * @param sessionId 会话 ID
     * @param userId    用户 ID
     * @return 消息列表
     */
    public List<ChatMessage> getSessionMessages(String sessionId, Integer userId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getUserId, userId)
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime);
        return chatMessageMapper.selectList(wrapper);
    }

    // ========== 私有方法 ==========

    /**
     * 保存聊天消息
     */
    private void saveMessage(Integer userId, String sessionId, String role, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setUserId(userId);
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(msg);
    }

    /**
     * 构建 LLM API 的消息数组
     * <p>
     * 结构:
     *   system: "你是一个专业的户外徒步助手……" + 检索到的上下文
     *   ...历史消息（最近 N 轮）...
     *   user: 当前问题
     *
     * @param question     用户问题
     * @param relevantDocs 检索到的相关文档
     * @param sessionId    会话 ID
     * @param userId       用户 ID
     * @return 消息数组
     */
    private JSONArray buildPrompt(String question, List<KnowledgeEmbedding> relevantDocs,
                                   String sessionId, Integer userId) {
        JSONArray messages = new JSONArray();

        // --- System 消息 ---
        StringBuilder systemContent = new StringBuilder();
        systemContent.append("你是一个资深的户外徒步旅行专家，名叫\"行山助手\"，隶属于\"行山记\"徒步社区。\n");
        systemContent.append("你的核心任务是基于社区博客文章中的真实经验，为用户提供专业、详细、实用的徒步建议。\n\n");

        systemContent.append("## 回答规范\n");
        systemContent.append("1. **回答必须详细充实**：不要只说一两句话，要展开说明，给出具体建议、理由和注意事项\n");
        systemContent.append("2. **基于知识库回答**：优先引用下方给出的参考文章内容，但不要直接说\"根据参考文章\"，而是把内容自然融入回答\n");
        systemContent.append("3. **结构化呈现**：使用 Markdown 格式，合理使用标题、列表、表格来组织内容\n");
        systemContent.append("4. **知识库不足时**：如果检索到的内容不足以回答问题，可以结合你的专业知识补充，但标注\"这是我的一般建议，社区知识库中暂无相关内容\"\n");
        systemContent.append("5. **语言风格**：用中文回答，语气亲切专业，像一位有经验的户外向导在分享经验\n\n");

        systemContent.append("## 知识库参考内容\n\n");

        if (!relevantDocs.isEmpty()) {
            systemContent.append("以下是从社区博客中检索到的相关内容，请优先参考这些内容来回答用户的问题：\n\n");
            for (int i = 0; i < relevantDocs.size(); i++) {
                String chunk = relevantDocs.get(i).getChunkText();
                // 如果 chunk 以 "标题:" 开头，提取标题
                if (chunk.startsWith("标题:")) {
                    int titleEnd = chunk.indexOf("\n");
                    String title = titleEnd > 0 ? chunk.substring(0, titleEnd) : chunk;
                    String content = titleEnd > 0 ? chunk.substring(titleEnd + 1) : "";
                    systemContent.append("### ").append(title.replace("标题:", "").trim()).append("\n");
                    if (!content.isBlank()) {
                        systemContent.append(content.replace("内容:", "").trim()).append("\n\n");
                    }
                } else {
                    systemContent.append(chunk).append("\n\n");
                }
            }
        } else {
            systemContent.append("（当前知识库中没有直接匹配的内容，你可以根据自己的专业知识回答，并在最后提醒用户知识库中暂无相关内容）\n");
        }

        JSONObject systemMsg = new JSONObject();
        systemMsg.set("role", "system");
        systemMsg.set("content", systemContent.toString());
        messages.add(systemMsg);

        // --- 历史消息 ---
        List<ChatMessage> history = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getUserId, userId)
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreateTime)
                        .last("LIMIT " + (aiConfig.getMaxHistory() * 2))
        );

        for (ChatMessage msg : history) {
            JSONObject histMsg = new JSONObject();
            histMsg.set("role", msg.getRole());
            histMsg.set("content", msg.getContent());
            messages.add(histMsg);
        }

        // --- 当前用户问题 ---
        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");
        userMsg.set("content", question);
        messages.add(userMsg);

        return messages;
    }

    /**
     * 解析流式响应中的 delta 内容
     * <p>
     * 预期格式: {"choices":[{"delta":{"content":"text"}}]}
     *
     * @param jsonData JSON 数据字符串
     * @return delta 内容，如果没有则返回 null
     */
    private String parseDeltaContent(String jsonData) {
        try {
            JSONObject obj = new JSONObject(jsonData);
            JSONArray choices = obj.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
            if (delta == null) {
                return null;
            }
            return delta.getStr("content");
        } catch (Exception e) {
            log.debug("解析 delta 内容失败: {}", jsonData);
            return null;
        }
    }
}