package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * <p>
 * 实体类
 * </p>
 */
@Data
@TableName(value = "blog")
public class Blog {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String name;

    private Integer typeId;

    private Integer userId;

    private String time;

    private String img;

    private String video;

    private String category;

    private String content;
    @TableField(exist = false)
    private  Integer count;
    @TableField(exist = false)
    private  Boolean isCollected;
}
