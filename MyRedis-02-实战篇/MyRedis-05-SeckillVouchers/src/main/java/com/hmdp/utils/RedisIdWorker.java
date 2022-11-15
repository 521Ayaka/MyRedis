package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    /**
     * 开始的时间戳 2022年1月1日0时0分
     */
    public static final long BEGIN_TIMESTAMP = 1640995200L;
    /**
     * 移动的位数 32位
     */
    public static final int COUNT_BITS = 32;
    /**
     * 自动注入StringRedisTemplate
     */
    private StringRedisTemplate stringRedisTemplate;
    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 获取全局唯一id
     * @param keyPrefix
     * @return
     */
    public long nextId(String keyPrefix){

        //获取当前的时间戳
        LocalDateTime time = LocalDateTime.now();
        long nowTimestamp = time.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowTimestamp - BEGIN_TIMESTAMP;

        //获取Redis自增数值
        //1.获取时间日期 给redis key分组 精确到天
        String format = time.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //2.获取自增数
        Long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + format);

        //进行计算并返回
        return timestamp << COUNT_BITS | count;
    }



/*
public static void main(String[] args) {
    LocalDateTime begin = LocalDateTime.of(2022, 1, 1, 0, 0);
    long beginTimestamp = begin.toEpochSecond(ZoneOffset.UTC);
    System.out.println(beginTimestamp);
}
*/

}
