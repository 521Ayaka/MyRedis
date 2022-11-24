package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    /**
     * 根据用户id 修改关注 取消关注
     * @param id
     * @param isFollow
     * @return
     */
    @PutMapping("/{id}/{isFollow}")
    public Result followUser(@PathVariable("id") Long id, @PathVariable("isFollow") boolean isFollow){
        return followService.followUser(id,isFollow);
    }

    /**
     * 根据 id 判断是否关注
     * @param id
     * @return
     */
    @GetMapping("/or/not/{id}")
    public Result notFollow(@PathVariable("id") Long id){
        return followService.notFollow(id);
    }

    /**
     * 共同关注查询
     * @param id
     * @return
     */
    @GetMapping("/common/{id}")
    public Result followCommon(@PathVariable("id") Long id){
        return followService.followCommon(id);
    }

}
