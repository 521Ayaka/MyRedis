package com.example;

import com.example.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class RedisApplicationTests {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    void contextLoads() {
        redisTemplate.opsForValue().set("xxx","yyy");

        Object xxx = redisTemplate.opsForValue().get("xxx");
        System.out.println("xxx = " + xxx);

    }

    @Test
    void UserTest(){

        User user = new User("尴尬酱", 20);
        redisTemplate.opsForValue().set("user1",user);

        Object user1 = redisTemplate.opsForValue().get("user1");
        System.out.println(user1);

    }

}
