package com.example.springboot.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Blog;
import com.example.springboot.entity.Collect;
import com.example.springboot.entity.Follow;
import com.example.springboot.entity.User;
import com.example.springboot.service.IBlogService;
import com.example.springboot.service.ICollectService;
import com.example.springboot.service.IFollowService;
import com.example.springboot.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;
    @Resource
    private IFollowService followService;
    @Resource
    private ICollectService collectService;
    @Resource
    private IBlogService blogService;

    @PostMapping
    public Result save(@RequestBody User user) {
        return Result.success(userService.saveOrUpdate(user));
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        return Result.success(userService.removeById(id));
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        return Result.success(userService.removeByIds(ids));
    }

    @GetMapping
    public Result findAll() {
        return Result.success(userService.list());
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        return Result.success(userService.getById(id));
    }

    @GetMapping("/info/{userId}")
    public Result info(@PathVariable Integer userId) {
        // 创建一个 JSON 对象来存储结果
        JSONObject object = new JSONObject();

        // 获取关注用户数
        LambdaQueryWrapper<Follow> followWrapper = new LambdaQueryWrapper<>();
        followWrapper.eq(Follow::getUserId, userId);
        long followCount = followService.count(followWrapper);

        // 获取粉丝数
        LambdaQueryWrapper<Follow> followerWrapper = new LambdaQueryWrapper<>();
        followerWrapper.eq(Follow::getItemId, userId);
        long followerCount = followService.count(followerWrapper);

        // 获取该用户发布的博客
        LambdaQueryWrapper<Blog> blogWrapper = new LambdaQueryWrapper<>();
        blogWrapper.eq(Blog::getUserId, userId);
        List<Blog> blogs = blogService.list(blogWrapper);

        // 计算获赞数和获收藏数
        long collectCount = 0;

        for (Blog blog : blogs) {
            // 统计每篇博文的获收藏数
            LambdaQueryWrapper<Collect> collectWrapper = new LambdaQueryWrapper<>();
            collectWrapper.eq(Collect::getItemId, blog.getId());
            collectCount += collectService.count(collectWrapper);
        }

        // 将结果存入 JSON 对象
        object.put("followCount", followCount);
        object.put("followerCount", followerCount);
        object.put("collectCount", collectCount);

        return Result.success(object);
    }



    @GetMapping("/page")
    public Result findPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(User::getId);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(User::getNickname, keyword);
        }

        return Result.success(userService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }

}

