package com.example.springboot.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.*;
import com.example.springboot.service.IBlogService;
import com.example.springboot.service.ICollectService;
import com.example.springboot.service.IMessageService;
import com.example.springboot.service.IUserService;
import com.example.springboot.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/collect")
public class CollectController {

    @Resource
    private ICollectService collectService;
    @Resource
    private IMessageService messageService;
    @Resource
    private IBlogService blogService;

    @PostMapping
    public Result save(@RequestBody Collect collect) {
        Account currentUser = TokenUtils.getCurrentUser();
        try {
            collect.setUserId(currentUser.getId());
            collectService.saveOrUpdate(collect);
        }catch (Exception e){
            LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Collect::getUserId,currentUser.getId());
            wrapper.eq(Collect::getItemId,collect.getItemId());
            collectService.remove(wrapper);
            return Result.error("605","取消收藏成功");
        }

        Message message = new Message();
        message.setText("收藏了你的笔记");
        message.setType("收藏");
        message.setTime(DateUtil.now());
        message.setFromUserId(currentUser.getId());
        Blog blog = blogService.getById(collect.getItemId());
        message.setToUserId(blog.getUserId());
        message.setItemId(collect.getItemId());
        messageService.save(message);

        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        Account currentUser = TokenUtils.getCurrentUser();
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId,currentUser.getId());
        wrapper.eq(Collect::getItemId,id);
        collectService.remove(wrapper);
        return Result.success();
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        return Result.success(collectService.removeByIds(ids));
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<Collect> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Collect::getId);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Collect::getUserId, keyword);
        }

        return Result.success(collectService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }

}

