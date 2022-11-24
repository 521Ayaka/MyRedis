package com.ganga.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.ganga.dto.Result;
import com.ganga.entity.Shop;
import com.ganga.mapper.ShopMapper;
import com.ganga.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ganga.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        /* 1. 获取商品id的key */
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        //2. 通过key向redis缓存中查询数据
        String shopStr = stringRedisTemplate.opsForValue().get(key);
        //3. 如果命中，返回查询结果
        if (ObjectUtil.isNotEmpty(shopStr)){
            Shop shop = JSONUtil.toBean(shopStr, Shop.class); //转成对象
            return Result.ok(shop);
        }
        //4. 如果未命中，查询数据库
        //5. 通过id查询数据库
        Shop shopById = this.getById(id);
        //6. 存在，写入redis缓存，并返回查询结果
        if (ObjectUtil.isNotEmpty(shopById)){
            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shopById));
            return Result.ok(shopById);
        }
        //7. 不存在，返回404
        return Result.fail("查询的商品不存在");
    }



}
