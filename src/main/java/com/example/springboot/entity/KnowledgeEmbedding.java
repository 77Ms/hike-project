package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_embedding")
public class KnowledgeEmbedding {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Integer blogId;

    private String chunkText;

    private String embedding;

    private LocalDateTime createTime;
}