package com.example.springboot.ai;

import com.example.springboot.entity.Blog;
import com.example.springboot.entity.KnowledgeEmbedding;
import com.example.springboot.mapper.KnowledgeEmbeddingMapper;
import com.example.springboot.service.IBlogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class KnowledgeSyncService {

    @Resource
    private IBlogService blogService;

    @Resource
    private KnowledgeEmbeddingMapper knowledgeEmbeddingMapper;

    @Resource
    private EmbeddingService embeddingService;

    /**
     * 同步所有博客到知识库：
     * 1. 清空现有知识库
     * 2. 遍历所有博客，为每篇博客生成嵌入
     * 3. 对于长内容，按段落或固定长度分块
     */
    @Transactional
    public void syncAll() {
        // 1. 清空现有知识库
        knowledgeEmbeddingMapper.delete(null);

        // 2. 获取所有博客
        List<Blog> blogs = blogService.list();

        if (blogs.isEmpty()) {
            log.warn("没有博客内容可供同步到知识库");
            return;
        }

        // 3. 为每篇博客分块并生成嵌入
        int totalChunks = 0;
        for (Blog blog : blogs) {
            String fullText = buildBlogText(blog);
            List<String> chunks = chunkText(fullText, 500);
            for (String chunk : chunks) {
                embeddingService.generateAndStore(blog.getId(), chunk);
                totalChunks++;
            }
        }

        log.info("知识库同步完成，共处理 {} 篇博客，生成 {} 个文本块", blogs.size(), totalChunks);
    }

    /**
     * 构建博客完整文本（标题 + 内容）
     */
    private String buildBlogText(Blog blog) {
        StringBuilder sb = new StringBuilder();
        if (blog.getName() != null) {
            sb.append("标题: ").append(blog.getName()).append("\n");
        }
        if (blog.getContent() != null) {
            sb.append("内容: ").append(blog.getContent());
        }
        return sb.toString();
    }

    /**
     * 将文本按最大长度分块，尽量在段落边界处分割
     *
     * @param text      原始文本
     * @param maxLength 每块最大字符数
     * @return 分块列表
     */
    private List<String> chunkText(String text, int maxLength) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        if (text.length() <= maxLength) {
            chunks.add(text);
            return chunks;
        }

        // 按段落分割
        String[] paragraphs = text.split("\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            // 如果单个段落超过最大长度，按字符强制分割
            if (paragraph.length() > maxLength) {
                // 先保存当前累积的内容
                if (!currentChunk.isEmpty()) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }
                // 按字符分割长段落
                for (int i = 0; i < paragraph.length(); i += maxLength) {
                    int end = Math.min(i + maxLength, paragraph.length());
                    chunks.add(paragraph.substring(i, end));
                }
                continue;
            }

            // 如果加上当前段落会超出最大长度，先保存当前块
            if (currentChunk.length() + paragraph.length() + 1 > maxLength) {
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder();
            }

            // 添加段落
            if (!currentChunk.isEmpty()) {
                currentChunk.append("\n");
            }
            currentChunk.append(paragraph);
        }

        // 保存最后一块
        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }
}