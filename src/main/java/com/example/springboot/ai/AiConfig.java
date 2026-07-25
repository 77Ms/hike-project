package com.example.springboot.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    /**
     * OpenAI 兼容 API 的基础地址
     */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * API 密钥
     */
    private String apiKey = "";

    /**
     * 聊天模型名称
     */
    private String model = "gpt-4o-mini";

    /**
     * 嵌入模型名称
     */
    private String embeddingModel = "text-embedding-ada-002";

    /**
     * 生成温度
     */
    private Double temperature = 0.7;

    /**
     * 最大生成 token 数
     */
    private Integer maxTokens = 2048;

    /**
     * 检索的 top-K 文档块数
     */
    private Integer topK = 3;

    /**
     * 携带的历史消息轮数（每轮含 user + assistant 两条消息）
     */
    private Integer maxHistory = 10;
}