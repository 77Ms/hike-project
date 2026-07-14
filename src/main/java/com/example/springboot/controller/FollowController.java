package com.example.springboot.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Blog;
import com.example.springboot.entity.Collect;
import com.example.springboot.entity.Follow;
import com.example.springboot.service.IBlogService;
import com.example.springboot.service.IFollowService;
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
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;
    @Resource
    private IBlogService blogService;

    @PostMapping
    public Result save(@RequestBody Follow follow) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (follow.getItemId().equals(currentUser.getId())) {
            return Result.error("605","不可以关注自己");
        }
        try {
            follow.setUserId(currentUser.getId());
            followService.saveOrUpdate(follow);
        }catch (Exception e){
            LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Follow::getUserId,currentUser.getId());
            wrapper.eq(Follow::getItemId,follow.getItemId());
            followService.remove(wrapper);
            return Result.error("605","取消关注成功");
        }
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        return Result.success(followService.removeById(id));
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        return Result.success(followService.removeByIds(ids));
    }

    @GetMapping
    public Result findAll() {
        return Result.success(followService.list());
    }

    @GetMapping("/check/{userId}")
    public Result check(@PathVariable Integer userId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getUserId, TokenUtils.getCurrentUser().getId());
        wrapper.eq(Follow::getItemId, userId);
        return followService.count(wrapper) > 0 ? Result.success() : Result.error("605", "尚未关注");
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        return Result.success(followService.getById(id));
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Follow::getId);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Follow::getUserId, keyword);
        }

        return Result.success(followService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }

}

