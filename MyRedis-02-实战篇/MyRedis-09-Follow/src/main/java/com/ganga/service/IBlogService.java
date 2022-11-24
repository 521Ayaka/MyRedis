package com.ganga.service;

import com.ganga.dto.Result;
import com.ganga.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IBlogService extends IService<Blog> {

    Result queryHotBlog(Integer current);

    Result queryByIdBlog(Long id);

    Result likeBlog(Long id);

    Result likesBlogTop(Long id);

    Result queryOfUserBlog(Long id, Integer current);

    Result queryMyBlog(Integer current);

    Result saveBlog(Blog blog);

}
