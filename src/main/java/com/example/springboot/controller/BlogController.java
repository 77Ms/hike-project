package com.example.springboot.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.*;
import com.example.springboot.service.IBlogService;
import com.example.springboot.service.ICollectService;
import com.example.springboot.service.ITypeService;
import com.example.springboot.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;
    @Resource
    private ICollectService collectService;
    @Resource
    private ITypeService typeService;


    @GetMapping("/count")
    public Result count(){

        List<Type> list = typeService.list();

        Map<Integer, Long> map = blogService.list().stream().collect(Collectors.groupingBy(Blog::getTypeId,Collectors.counting()));

        JSONArray array = new JSONArray();

        for (Type type : list) {
            JSONObject object = new JSONObject();
            object.set("name",type.getName());
            object.set("value",map.getOrDefault(type.getId(),0L));
            array.add(object);
        }

        return Result.success(array);
    }

    @PostMapping
    public Result save(@RequestBody Blog blog) {
        if (blog.getId()==null){
            blog.setUserId(TokenUtils.getCurrentUser().getId());
            blog.setTime(DateUtil.now());
        }
        return Result.success(blogService.saveOrUpdate(blog));
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        return Result.success(blogService.removeById(id));
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        return Result.success(blogService.removeByIds(ids));
    }

    @GetMapping
    public Result findAll() {
        Account account = TokenUtils.getCurrentUser();
        LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<>();
        if (!StrUtil.equals(account.getRole(),"ROLE_ADMIN")){
            wrapper.eq(Blog::getUserId,account.getId());
        }
        return Result.success(blogService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        return Result.success(blogService.getById(id));
    }

    @GetMapping("/front/page")
    public Result findFrontPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam Integer typeId,
                           @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<Blog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Blog::getId);

        if (typeId!=0){
            queryWrapper.eq(Blog::getTypeId,typeId);
        }

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Blog::getName, keyword);
        }

        Page<Blog> page = blogService.page(new Page<>(pageNum, pageSize), queryWrapper);

        Map<Integer, Long> map = collectService.list().stream().collect(Collectors.groupingBy(Collect::getItemId, Collectors.counting()));
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, TokenUtils.getCurrentUser().getId());
        List<Integer> collectList = collectService.list(wrapper).stream().map(Collect::getItemId).collect(Collectors.toList());

        for (Blog blog : page.getRecords()) {
            blog.setIsCollected(collectList.contains(blog.getId()));
            blog.setCount(Math.toIntExact(map.getOrDefault(blog.getId(), 0L)));
        }

        return Result.success(page);
    }

    @GetMapping("/user/page")
    public Result findUserPage(@RequestParam Integer pageNum,
                                @RequestParam Integer pageSize,
                                @RequestParam Integer userId,
                                @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<Blog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Blog::getId);

        queryWrapper.eq(Blog::getUserId,userId);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Blog::getName, keyword);
        }

        Page<Blog> page = blogService.page(new Page<>(pageNum, pageSize), queryWrapper);

        Map<Integer, Long> map = collectService.list().stream().collect(Collectors.groupingBy(Collect::getItemId, Collectors.counting()));
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, TokenUtils.getCurrentUser().getId());
        List<Integer> collectList = collectService.list(wrapper).stream().map(Collect::getItemId).collect(Collectors.toList());

        for (Blog blog : page.getRecords()) {
            blog.setIsCollected(collectList.contains(blog.getId()));
            blog.setCount(Math.toIntExact(map.getOrDefault(blog.getId(), 0L)));
        }

        return Result.success(page);
    }

    @GetMapping("/collect/page")
    public Result findCollectPage(@RequestParam Integer pageNum,
                               @RequestParam Integer pageSize,
                               @RequestParam Integer userId,
                               @RequestParam(defaultValue = "") String keyword) {


        LambdaQueryWrapper<Collect> collectWrapper = new LambdaQueryWrapper<>();
        collectWrapper.eq(Collect::getUserId, userId);
        List<Integer> ids = collectService.list(collectWrapper).stream().map(Collect::getItemId).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(ids)) return Result.success(collectService.page(new Page<>(),collectWrapper));

        LambdaQueryWrapper<Blog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Blog::getId);

        queryWrapper.in(Blog::getId,ids);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Blog::getName, keyword);
        }

        Page<Blog> page = blogService.page(new Page<>(pageNum, pageSize), queryWrapper);

        Map<Integer, Long> map = collectService.list().stream().collect(Collectors.groupingBy(Collect::getItemId, Collectors.counting()));
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, TokenUtils.getCurrentUser().getId());
        List<Integer> collectList = collectService.list(wrapper).stream().map(Collect::getItemId).collect(Collectors.toList());

        for (Blog blog : page.getRecords()) {
            blog.setIsCollected(collectList.contains(blog.getId()));
            blog.setCount(Math.toIntExact(map.getOrDefault(blog.getId(), 0L)));
        }

        return Result.success(page);
    }


    @GetMapping("/page")
    public Result findPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<Blog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Blog::getId);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Blog::getName, keyword);
        }

        Account account = TokenUtils.getCurrentUser();
        if (!StrUtil.equals(account.getRole(),"ROLE_ADMIN")){
            queryWrapper.eq(Blog::getUserId,account.getId());
        }

        return Result.success(blogService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }

}

