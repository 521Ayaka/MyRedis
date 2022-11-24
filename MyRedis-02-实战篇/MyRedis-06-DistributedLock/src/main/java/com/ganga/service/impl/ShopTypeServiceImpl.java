package com.ganga.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.ganga.dto.Result;
import com.ganga.entity.ShopType;
import com.ganga.mapper.ShopTypeMapper;
import com.ganga.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ganga.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {

        List<ShopType> shopTypes = new ArrayList<>();

        //1. 通过缓存redis中查询
        List<String> rangeList = stringRedisTemplate.opsForList().range(RedisConstants.CACHE_SHOP_TYPE, 0, -1);

        //2. 判断是否命中
        if (ObjectUtil.isNotEmpty(rangeList)){
            for (String args: rangeList) {
                ShopType shopType = JSONUtil.toBean(args, ShopType.class);
                shopTypes.add(shopType);
            }

            System.out.println("redis命中，使用redis缓存");

            return Result.ok(shopTypes);
        }

        System.out.println("未命中 通过数据库查询");

        //3. 未命中 通过数据库查询
        shopTypes = this.query().orderByAsc("sort").list();

        //4. 数据库中存在，存入redis缓存当中
        if (ObjectUtil.isNotEmpty(shopTypes)){
            //存入redis
            for (ShopType shop: shopTypes) {
                String json = JSONUtil.toJsonStr(shop);
                stringRedisTemplate.opsForList().rightPush(RedisConstants.CACHE_SHOP_TYPE,json);
            }

            //回应客户端
            return Result.ok(shopTypes);
        }

        //5. 数据库中不存在，404
        return Result.fail("错误！");
    }
}
