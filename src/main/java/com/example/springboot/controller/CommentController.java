package com.example.springboot.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Comment;
import com.example.springboot.entity.Type;
import com.example.springboot.entity.User;
import com.example.springboot.entity.Word;
import com.example.springboot.service.ICommentService;
import com.example.springboot.service.ITypeService;
import com.example.springboot.service.IUserService;
import com.example.springboot.service.IWordService;
import com.example.springboot.utils.DFAUtil;
import com.example.springboot.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/comment")
public class CommentController {

    @Resource
    private ICommentService commentService;
    @Resource
    private IUserService userService;
    @Resource
    private IWordService wordService;
    @Resource
    private DFAUtil dfaUtil;

//新增、更新
    @PostMapping
    public Result save(@RequestBody Comment comment) {

        if(comment.getId()==null){
            comment.setUserId(TokenUtils.getCurrentUser().getId());
            comment.setTime(DateUtil.now());
        }

        if (comment.getPid()!=null){
            Comment pComment = commentService.getById(comment.getPid());
            if(pComment.getOriginId()==null){
                comment.setOriginId(pComment.getId());
                comment.setPid(null);
            }else {
                comment.setOriginId(pComment.getOriginId());
            }
        }

        String[] words = wordService.list().stream().map(Word::getName).collect(Collectors.toList()).toArray(new String[0]);
        dfaUtil.addSensitiveWords(words);
        comment.setContent(dfaUtil.replace(comment.getContent(), '*'));


        return Result.success(commentService.saveOrUpdate(comment));
    }
//    删除
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {

        return Result.success(commentService.removeById(id));
    }
//    批量删除
    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        return Result.success(commentService.removeByIds(ids));
    }

    @GetMapping("/tree/{itemId}")
    public Result findTree(@PathVariable Integer itemId) {
        //首先查询出来所有用户信息，存为map
        Map<Integer, User> map = userService.list().stream().collect(Collectors.toMap(User::getId, u -> u));
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getItemId, itemId);
        wrapper.orderByDesc(Comment::getId);
        //拿到所有的评论信息
        List<Comment> allComments = commentService.list(wrapper).stream()
                .map(c -> {
                    //帮助所有的评论信息根据userId组装当前评论用户的名称和头像
                    Optional.ofNullable(map.get(c.getUserId())).ifPresent(user -> {
                        c.setNickname(user.getNickname());
                        c.setAvatarUrl(user.getAvatarUrl());
                    });
                    return c;
                })
                .collect(Collectors.toList());
        //查询出来所有最外层评论
        List<Comment> originList = allComments.stream().filter(comment -> comment.getOriginId() == null).collect(Collectors.toList());
        //遍历每一个最外层评论
        for (Comment origin : originList) {
            //找到所有最外层评论id为当前最外层评论的所有评论
            List<Comment> comments = allComments.stream().filter(comment -> origin.getId().equals(comment.getOriginId())).collect(Collectors.toList());
            //遍历这些子评论
            comments.forEach(comment -> {
                //找一下这些评论中是否有父评论
                Optional<Comment> pComment = allComments.stream().filter(c1 -> c1.getId().equals(comment.getPid())).findFirst();
                //如果存在父评论，设置一下父评论的昵称信息
                pComment.ifPresent((v -> {
                    comment.setPUserId(v.getUserId());
                    comment.setPNickname(v.getNickname());
                }));
            });
            //将已经组装好数据的评论设置为某一个最外层评论的字评论集合
            origin.setChildren(comments);
        }
        //返回所有最外层
        return Result.success(originList);
    }
    //    查所有
    @GetMapping
    public Result findAll() {
        return Result.success(commentService.list());
    }

//    查一个
    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        return Result.success(commentService.getById(id));
    }
//    分页查询
    @GetMapping("/page")
    public Result findPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Comment::getId);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Comment::getContent, keyword);
        }

        return Result.success(commentService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }

}

