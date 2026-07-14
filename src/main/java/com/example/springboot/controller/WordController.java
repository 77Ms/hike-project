package com.example.springboot.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Word;
import com.example.springboot.service.IWordService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/word")
public class WordController {

    @Resource
    private IWordService wordService;

    @PostMapping
    public Result save(@RequestBody Word word) {
        return Result.success(wordService.saveOrUpdate(word));
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        return Result.success(wordService.removeById(id));
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        return Result.success(wordService.removeByIds(ids));
    }

    @GetMapping
    public Result findAll() {
        return Result.success(wordService.list());
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        return Result.success(wordService.getById(id));
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<Word> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Word::getId);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Word::getName, keyword);
        }

        return Result.success(wordService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }

}

