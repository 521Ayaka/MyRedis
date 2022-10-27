package com.example;

import com.example.config.RedisTemplateConfig;
import com.example.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest(classes = SpringBootRedisJedisDame.class)
@Import(RedisTemplateConfig.class)
public class RedisApplicationTests2 {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    void contextLoads() {
        redisTemplate.opsForValue().set("AAA","yyy");

        Object xxx = redisTemplate.opsForValue().get("AAA");
        System.out.println("AAA = " + xxx);

    }

    @Test
    void UserTest(){

        User user = new User("尴尬酱", 20);
        redisTemplate.opsForValue().set("user2",user);

        Object user2 = redisTemplate.opsForValue().get("user2");
        System.out.println(user2);

    }


}
