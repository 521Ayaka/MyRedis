package com.ganga.service.impl;

import com.ganga.entity.BlogComments;
import com.ganga.service.IBlogCommentsService;
import com.ganga.mapper.BlogCommentsMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
