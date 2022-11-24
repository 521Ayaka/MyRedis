package com.ganga.service;

import com.ganga.dto.Result;
import com.ganga.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IFollowService extends IService<Follow> {

    Result followUser(Long id, boolean isFollow);

    Result notFollow(Long id);

    Result followCommon(Long id);
}
