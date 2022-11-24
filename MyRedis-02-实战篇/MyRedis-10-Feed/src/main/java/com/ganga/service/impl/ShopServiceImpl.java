package com.ganga.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.ganga.entity.Shop;
import com.ganga.mapper.ShopMapper;
import com.ganga.service.IShopService;
import com.ganga.utils.CacheClient;
import com.ganga.utils.RedisConstants;
import com.ganga.dto.Result;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {

        //使用缓存
        //Result result = queryByIdCache(id);

        //使用缓存解决 [缓存穿透] 问题
        /*Shop shop = cacheClient.queryWithPassThrough(
                RedisConstants.CACHE_SHOP_KEY,
                id, Shop.class,
                (ids)-> getById(ids),
                //this::getById, //简化使用Lambda表达式
                RedisConstants.CACHE_SHOP_TTL,
                TimeUnit.MINUTES,
                RedisConstants.CACHE_SHOP_NOLL_TTL,
                TimeUnit.SECONDS);*/

        //使用缓存解决 [缓存击穿] 问题 方案一： [互斥锁方案]
        Shop shop = cacheClient.queryWithMutex(
                RedisConstants.CACHE_SHOP_KEY,
                RedisConstants.LOCK_SHOP_KEY,
                RedisConstants.CACHE_SHOP_TTL,TimeUnit.MINUTES,
                RedisConstants.CACHE_SHOP_NOLL_TTL,TimeUnit.SECONDS,
                RedisConstants.LOCK_SHOP_TTL,TimeUnit.SECONDS,
                id, Shop.class, this::getById);
        //使用缓存解决 [缓存击穿] 问题 方案一： [逻辑过期方案]

        if (shop == null){
            return Result.fail("查询的商品不存在！");
        }

        return Result.ok(shop);
    }















    public Result queryByIdCacheMutex(Long id) {

        //1. 获取商品id的key
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        //2. 通过key向redis缓存中查询数据
        String shopStr = stringRedisTemplate.opsForValue().get(key);
        //3. 如果命中，返回查询结果
        if (ObjectUtil.isNotEmpty(shopStr)) {
            Shop shop = JSONUtil.toBean(shopStr, Shop.class); //转成对象
            return Result.ok(shop);
        }

        //3.1 判断是否命中的是空值
        if (shopStr == "") {
            return null;
        }

        //4. 如果未命中，进行缓存重建
        try {

            //4.1 获取互斥锁
            boolean lock = setShopMutex(id);

            //4.2 判断互斥锁是否获取成功 不成功，等待 + 递归
            if (!lock) {
                Thread.sleep(500); //等到500毫秒
                queryByIdCacheMutex(id); //递归
            }

            //4.3 获取互斥锁成功 通过id查询数据库
            Shop shopById = this.getById(id);

            //4.4 存在，写入redis缓存，并返回查询结果
            if (ObjectUtil.isNotEmpty(shopById)) {
                stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shopById));
                return Result.ok(shopById);
            }

            //4.5 不存在，向redis写入空对象
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_NOLL, "", RedisConstants.CACHE_SHOP_NOLL_TTL, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {

            //4.6 释放互斥锁
            delShopMutex(id);
        }

        //5. 向前端返回信息
        return Result.fail("查询的商品不存在");
    }


    //获取互斥锁
    public boolean setShopMutex(Long id) {
        //使用 .setIfAbsent()方法
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(RedisConstants.LOCK_SHOP_KEY + id, "lock", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(lock);
    }

    //删除互斥锁
    public void delShopMutex(Long id) {
        stringRedisTemplate.delete(RedisConstants.LOCK_SHOP_KEY + id);
    }



}
