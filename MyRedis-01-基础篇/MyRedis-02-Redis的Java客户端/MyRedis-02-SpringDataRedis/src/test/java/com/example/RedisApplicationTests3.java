package com.example;

import com.example.domain.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
public class RedisApplicationTests3 {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void contextLoads() {
        redisTemplate.opsForValue().set("AAA", "yyy");

        Object xxx = redisTemplate.opsForValue().get("AAA");
        System.out.println("AAA = " + xxx);

    }

    @Test
    void UserTest() throws JsonProcessingException {

        User user = new User("尴尬酱", 20);
        //序列化对象json写入
        redisTemplate.opsForValue().set("user2", objectMapper.writeValueAsString(user));

        String user2 = redisTemplate.opsForValue().get("user2");
        //手动反序列化
        User user2x = objectMapper.readValue(user2, User.class);
        System.out.println("user2  ---> " + user2);
        System.out.println("user2x ---> " + user2x);

    }

}
