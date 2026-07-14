package com.example.springboot.ai;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.example.springboot.entity.KnowledgeEmbedding;
import com.example.springboot.mapper.KnowledgeEmbeddingMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class EmbeddingService {

    @Resource
    private KnowledgeEmbeddingMapper knowledgeEmbeddingMapper;

    @Resource
    private LlmClient llmClient;

    @Resource
    private AiConfig aiConfig;

    /**
     * 为单段文本生成嵌入并存储到数据库
     */
    public KnowledgeEmbedding generateAndStore(Integer blogId, String chunkText) {
        float[] vector = llmClient.createEmbedding(chunkText);

        KnowledgeEmbedding ke = new KnowledgeEmbedding();
        ke.setBlogId(blogId);
        ke.setChunkText(chunkText);
        ke.setEmbedding(vectorToJson(vector));
        ke.setCreateTime(LocalDateTime.now());

        knowledgeEmbeddingMapper.insert(ke);
        return ke;
    }

    /**
     * 语义搜索：对用户问题生成嵌入，与所有存储的嵌入进行余弦相似度比较
     *
     * @param question 用户问题
     * @return topK 个最相似的 KnowledgeEmbedding
     */
    public List<KnowledgeEmbedding> semanticSearch(String question) {
        float[] queryVector = llmClient.createEmbedding(question);
        List<KnowledgeEmbedding> all = knowledgeEmbeddingMapper.selectList(null);

        if (all.isEmpty()) {
            return new ArrayList<>();
        }

        List<SimilarityResult> scored = new ArrayList<>();
        for (KnowledgeEmbedding ke : all) {
            float[] storedVector = parseEmbedding(ke.getEmbedding());
            double score = cosineSimilarity(queryVector, storedVector);
            scored.add(new SimilarityResult(ke, score));
        }

        scored.sort(Comparator.comparingDouble(SimilarityResult::score).reversed());

        int topK = Math.min(aiConfig.getTopK(), scored.size());
        List<KnowledgeEmbedding> result = new ArrayList<>();
        for (int i = 0; i < topK; i++) {
            result.add(scored.get(i).embedding);
        }

        log.debug("语义搜索完成，找到 {} 个相关文档，top-1 得分: {}", result.size(),
                scored.isEmpty() ? 0 : scored.get(0).score);

        return result;
    }

    /**
     * 将 float[] 转为 JSON 字符串
     */
    private String vectorToJson(float[] vector) {
        JSONArray arr = new JSONArray();
        for (float v : vector) {
            arr.add(v);
        }
        return arr.toString();
    }

    /**
     * 将 JSON 字符串解析为 float[]
     */
    private float[] parseEmbedding(String json) {
        JSONArray arr = JSONUtil.parseArray(json);
        float[] result = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            result[i] = arr.getDouble(i).floatValue();
        }
        return result;
    }

    /**
     * 计算两个向量的余弦相似度
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            log.warn("向量维度不匹配: {} vs {}", a.length, b.length);
            return 0;
        }

        double dotProduct = 0;
        double normA = 0;
        double normB = 0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0) {
            return 0;
        }

        return dotProduct / denominator;
    }

    private record SimilarityResult(KnowledgeEmbedding embedding, double score) {
    }
}