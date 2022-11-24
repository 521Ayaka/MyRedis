package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.service.IBlogService;
import com.hmdp.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

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


    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        //保存博客 并推送Feed流
        return blogService.saveBlog(blog);
    }


    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        // 修改点赞数量
        return blogService.likeBlog(id);
    }

    @GetMapping("/likes/{id}")
    public Result likesBlogTop(@PathVariable("id") Long id){
        // 点赞榜单 前 5 位
        return blogService.likesBlogTop(id);
    }


    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryMyBlog(current);
    }

    @GetMapping("/of/user")
    public Result queryOfUserBlog(@RequestParam("id") Long id,
                                  @RequestParam(value = "current", defaultValue = "1") Integer current){
        return blogService.queryOfUserBlog(id, current);
    }

    /**
     * 分页查询当前用户 关注用户的博客
     *      Feed流 取
     * @param maxTime
     * @param offset
     * @return
     */
    @GetMapping("/of/follow")
    private Result queryOfFollow(@RequestParam("lastId") Long maxTime,
                                 @RequestParam(value = "offset",defaultValue = "1") Integer offset){
        return blogService.queryOfFollow(maxTime,offset);
    }


    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }

    @GetMapping("/{id}")
    public Result queryByIdBlog(@PathVariable Long id){
        return blogService.queryByIdBlog(id);
    }


}
