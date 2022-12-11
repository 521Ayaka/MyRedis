package com.ganga.service;

import com.ganga.dto.Result;
import com.ganga.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IShopService extends IService<Shop> {

    Result queryById(Long id);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
