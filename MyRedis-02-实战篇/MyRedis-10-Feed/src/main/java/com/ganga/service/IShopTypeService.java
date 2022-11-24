package com.ganga.service;

import com.ganga.entity.ShopType;
import com.ganga.dto.Result;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IShopTypeService extends IService<ShopType> {

    Result queryTypeList();

}
