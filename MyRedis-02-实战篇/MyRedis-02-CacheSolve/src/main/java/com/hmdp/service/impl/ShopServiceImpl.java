package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.events.Event;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    //自定义线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    @Override
    public Result queryById(Long id) {

        //使用缓存
        //Result result = queryByIdCache(id);

        //使用缓存解决 [缓存穿透] 问题
        //Result result = queryByIdCachePenetration(id);

        //使用缓存解决 [缓存击穿] 问题 方案一： [互斥锁方案]
        Result result = queryByIdCacheMutex(id);

        //使用缓存解决 [缓存击穿] 问题 方案一： [逻辑过期方案]
        //Result result = queryByIdCacheLogicalExpire(id);

        //7. 不存在，返回404
        return result;
    }


    /**
     * 使用缓存
     *
     * @param id
     * @return
     */
    public Result queryByIdCache(Long id) {
        /* 1. 获取商品id的key */
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        //2. 通过key向redis缓存中查询数据
        String shopStr = stringRedisTemplate.opsForValue().get(key);
        //3. 如果命中，返回查询结果
        if (ObjectUtil.isNotEmpty(shopStr)) {
            Shop shop = JSONUtil.toBean(shopStr, Shop.class); //转成对象
            return Result.ok(shop);
        }
        //4. 如果未命中，查询数据库
        //5. 通过id查询数据库
        Shop shopById = this.getById(id);
        //6. 存在，写入redis缓存，并返回查询结果
        if (ObjectUtil.isNotEmpty(shopById)) {
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shopById));
            return Result.ok(shopById);
        }
        //7. 不存在，返回404
        return Result.fail("查询的商品不存在");
    }


    /**
     * 解决缓存穿透问题
     * 使用 缓存空对象 解决方案
     *
     * @param id
     * @return
     */
    public Result queryByIdCachePenetration(Long id) {

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
        if (shopStr != null) {
            return null;
        }

        //4. 如果未命中，查询数据库
        //5. 通过id查询数据库
        Shop shopById = this.getById(id);
        //6. 存在，写入redis缓存，并返回查询结果
        if (ObjectUtil.isNotEmpty(shopById)) {
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shopById));
            return Result.ok(shopById);
        }

        //7. 不存在，向redis写入空对象
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, "", RedisConstants.CACHE_SHOP_NOLL_TTL, TimeUnit.SECONDS);

        //8. 向前端返回信息
        return Result.fail("查询的商品不存在");
    }


    /**
     * 解决缓存击穿问题
     * 使用 互斥锁解决方案
     *
     * @param id
     * @return
     */
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
        if (shopStr != null) {
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

            //重点注意：
            //并发问题： 如果以一个刚释放所 前面没有命中 会重新建立缓存 解决方案就是 再次判断是否命中！
            String shopStrNew = stringRedisTemplate.opsForValue().get(key);
            if (ObjectUtil.isNotEmpty(shopStrNew)) {
                Shop shop = JSONUtil.toBean(shopStrNew, Shop.class); //转成对象
                return Result.ok(shop);
            }


            //4.3 获取互斥锁成功 通过id查询数据库
            Shop shopById = this.getById(id);

            //4.4 存在，写入redis缓存，并返回查询结果
            if (ObjectUtil.isNotEmpty(shopById)) {
                stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shopById));
                return Result.ok(shopById);
            }

            //4.5 不存在，向redis写入空对象
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, "", RedisConstants.CACHE_SHOP_NOLL_TTL, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {

            //4.6 释放互斥锁
            delShopMutex(id);
        }

        //5. 向前端返回信息
        return Result.fail("查询的商品不存在");
    }


    /**
     * 解决缓存击穿问题
     * 使用 逻辑过期解决方案
     *
     * @param id
     * @return
     */
    public Result queryByIdCacheLogicalExpire(Long id) {

        //1. 查询缓存是否命中
        String jsonData = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        //2. 未命中直接返回前端信息
        if (ObjectUtil.isEmpty(jsonData)) {
            return Result.fail("查询的商品不存在！");
        }
        //3. 命中
        //4. 反序列化数据
        RedisData redisData = JSONUtil.toBean(jsonData, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData(); //转成 JSONObject对象
        Shop shop = JSONUtil.toBean(data, Shop.class); //转成 Shop 对象
        LocalDateTime expireTime = redisData.getExpireTime(); //获取逻辑时间
        //5. 判断逻辑时间是否过去
        if (expireTime.isAfter(LocalDateTime.now())) { //expireTime.isAfter(LocalDateTime.now()) --> true:未过期  false：过期
            //6. 逻辑时间未过期 直接向前端返回数据
            return Result.ok(shop);
        }
        //7. 逻辑时间过期 尝试获取互斥锁
        boolean lock = setShopMutex(id);
        //8. 获取互斥成功
        if (lock) {
            //9. 开启新的线程
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    //10. 重建缓存
                    saveShopRedis(id, 10L);
                } finally {
                    //11. 释放锁
                    delShopMutex(id);
                }
            });
        }

        //12. 返回旧数据
        return Result.ok(shop);
    }


    //缓存重建
    public void saveShopRedis(Long id, Long expireSeconds) {

        //模拟重建时间
        System.out.println("休眠中");
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("休眠结束");

        //查询数据库数据
        Shop shop = this.getById(id);
        //封装 数据+逻辑时间
        RedisData redisData = new RedisData();
        redisData.setData(shop); //数据
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds)); //时间
        //序列化
        String json = JSONUtil.toJsonStr(redisData);
        //写入redis缓存当中
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, json);
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
