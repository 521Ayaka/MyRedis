package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class LockImpl implements ILock{

    /**
     * redis
     */
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 锁名称
     */
    private String name;
    public LockImpl(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    /**
     * 锁前缀
     */
    private static final String KEY_PREFIX = "lock:";

    /**
     * 锁的唯一标识
     */
    private String ID_PREFIX = UUID.randomUUID().toString(true);

    /**
     * 初始化Lua脚本     RedisScript的实现类
     */
    private static final  DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        //创建 RedisScript的实现类 DefaultRedisScript
        UNLOCK_SCRIPT = new DefaultRedisScript<Long>();
        //设置Lua脚本位置
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        //设置脚本执行后的返回值类型
        UNLOCK_SCRIPT.setResultType(Long.class);
    }



    /**
     * 尝试获取锁
     * @param timeoutSec 兜底过期时间
     * @return 获取是否成功 true成功
     */
    @Override
    public boolean tryLock(long timeoutSec) {
        // 锁的唯一标识：这里用 UUID + 线程id
        String value = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁的 key
        String key = KEY_PREFIX + name;

        //尝试获取锁
        Boolean isLock = stringRedisTemplate
                .opsForValue()
                .setIfAbsent(key, value, timeoutSec, TimeUnit.SECONDS);

        //返回结果
        //return Boolean.TRUE.equals(isLock); //或者 👇
        return BooleanUtil.isTrue(isLock);
    }


    /**
     * 释放锁
     */
    @Override
    public void unLock() {

        //判断将要释放的锁 的 线程表示是否一致 解决分布式锁误删问题

        //锁的唯一标识：这里用 UUID + 线程id
        String value = ID_PREFIX + Thread.currentThread().getId();
        //获取锁的 key
        String key = KEY_PREFIX + name;

        //判断将要释放的锁 的 线程表示是否一致 解决分布式锁误删问题
        //使用Lua脚本 确保 [判断标识] 和 [释放锁] 的 原子性
        stringRedisTemplate
                .execute(UNLOCK_SCRIPT, //Lua脚本对象
                         Collections.singletonList(key), //KEYS[1] list
                         value); //ARGV[1] object

        //否则 不释放锁
    }


    /**
     * 释放锁
     *//*
    @Override
    public void unLock() {

        //判断将要释放的锁 的 线程表示是否一致 解决分布式锁误删问题

        //锁的唯一标识：这里用 UUID + 线程id
        String value = ID_PREFIX + Thread.currentThread().getId();
        //获取锁的 key
        String key = KEY_PREFIX + name;
        //获取锁的标识
        String value2 = stringRedisTemplate.opsForValue().get(key);

        //判断将要释放的锁 的 线程表示是否一致 解决分布式锁误删问题
        if (value.equals(value2)){
            //释放锁
            stringRedisTemplate.delete(key);
        }
        //否则 不释放锁
    }*/

}
