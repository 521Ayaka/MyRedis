package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class RedisApplicationTests4 {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void hashTest(){

        redisTemplate.opsForHash().put("boot:hash:111","k1","123");
        redisTemplate.opsForHash().put("boot:hash:111","k2","321");

        Map<String, String> map = new HashMap<>();
        map.put("k1","321");
        map.put("k2","123");
        redisTemplate.opsForHash().putAll("boot:hash:222",map);

        Object k1 = redisTemplate.opsForHash().get("boot:hash:111", "k1");
        Object k2 = redisTemplate.opsForHash().get("boot:hash:111", "k2");
        System.out.println(k1 + "\n" + k2);

        Map<Object, Object> entries = redisTemplate.opsForHash().entries("boot:hash:222");

        for (Map.Entry<Object,Object> entry: entries.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
    }

}
