package com.ganga;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.ObjectUtils;

@SpringBootTest(classes = HyperLogLog.class)
public class UVTests {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Redis 实现 UV 统计
     * 生成 10万 条数据写入Redis 并统计
     */
    @Test
    void uvTest(){

        String[] array = new String[1000];
        int j = 0;
        for (int i = 0; i < 100000; i++) {
            j = i % 1000;
            array[j] = "user_id:" + i;
            if (j == 999){
                stringRedisTemplate.opsForHyperLogLog().add("uv:user",array);
            }
        }

        if (!ObjectUtils.isEmpty(array)){
            stringRedisTemplate.opsForHyperLogLog().add("uv:user",array);
        }

    }

    @Test
    void uvGetTest(){
        Long size = stringRedisTemplate.opsForHyperLogLog().size("uv:user");
        System.out.println(size);
    }

    @Test
    void uvRmTest(){
        stringRedisTemplate.delete("uv:user");
    }

    
}
