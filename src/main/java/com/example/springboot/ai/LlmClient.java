package com.example.springboot.ai;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.example.springboot.exception.ServiceException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LlmClient {

    @Resource
    private AiConfig aiConfig;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * 生成文本嵌入向量
     * POST {baseUrl}/embeddings
     *
     * @param input 输入文本
     * @return 嵌入向量 float[]
     */
    public float[] createEmbedding(String input) {
        String url = aiConfig.getBaseUrl() + "/embeddings";

        JSONObject body = new JSONObject();
        body.set("input", input);
        body.set("model", aiConfig.getEmbeddingModel());

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + aiConfig.getApiKey())
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "unknown error";
                log.error("Embedding API error: status={}, body={}", response.code(), errorBody);
                throw new ServiceException("500", "Embedding API 调用失败: " + response.code());
            }

            String json = response.body() != null ? response.body().string() : "{}";
            JSONObject jsonObj = new JSONObject(json);

            JSONArray data = jsonObj.getJSONArray("data");
            if (data == null || data.isEmpty()) {
                throw new ServiceException("500", "Embedding API 返回空数据");
            }

            JSONArray embeddingArr = data.getJSONObject(0).getJSONArray("embedding");
            float[] embedding = new float[embeddingArr.size()];
            for (int i = 0; i < embeddingArr.size(); i++) {
                embedding[i] = embeddingArr.getDouble(i).floatValue();
            }
            return embedding;

        } catch (IOException e) {
            log.error("Embedding API 请求异常", e);
            throw new ServiceException("500", "Embedding API 请求异常: " + e.getMessage());
        }
    }

    /**
     * 非流式聊天补全
     * POST {baseUrl}/chat/completions
     *
     * @param messages 消息数组（包含 system, user, assistant 角色）
     * @return AI 回答内容
     */
    public String chatCompletion(JSONArray messages) {
        String url = aiConfig.getBaseUrl() + "/chat/completions";

        JSONObject body = new JSONObject();
        body.set("model", aiConfig.getModel());
        body.set("messages", messages);
        body.set("temperature", aiConfig.getTemperature());
        body.set("max_tokens", aiConfig.getMaxTokens());

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + aiConfig.getApiKey())
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "unknown error";
                log.error("Chat completion API error: status={}, body={}", response.code(), errorBody);
                throw new ServiceException("500", "AI 服务调用失败: " + response.code());
            }

            String json = response.body() != null ? response.body().string() : "{}";
            JSONObject jsonObj = new JSONObject(json);

            JSONArray choices = jsonObj.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new ServiceException("500", "AI 返回空结果");
            }

            return choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getStr("content", "");

        } catch (IOException e) {
            log.error("Chat completion API 请求异常", e);
            throw new ServiceException("500", "AI 服务请求异常: " + e.getMessage());
        }
    }

    /**
     * 流式聊天补全
     * POST {baseUrl}/chat/completions 带 stream: true
     * 返回 OkHttp Response 对象，调用方需自行读取和关闭
     *
     * @param messages 消息数组
     * @return OkHttp Response（调用方负责关闭）
     */
    public Response streamingChatCompletion(JSONArray messages) {
        String url = aiConfig.getBaseUrl() + "/chat/completions";

        JSONObject body = new JSONObject();
        body.set("model", aiConfig.getModel());
        body.set("messages", messages);
        body.set("temperature", aiConfig.getTemperature());
        body.set("max_tokens", aiConfig.getMaxTokens());
        body.set("stream", true);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + aiConfig.getApiKey())
                .addHeader("Accept", "text/event-stream")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try {
            return httpClient.newCall(request).execute();
        } catch (IOException e) {
            log.error("Streaming chat completion 请求异常", e);
            throw new ServiceException("500", "AI 流式服务请求异常: " + e.getMessage());
        }
    }
}