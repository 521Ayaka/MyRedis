package com.ganga.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Redisson 初始化配置
 */
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient1() {

        Config config = new Config();
        //链接Redis
        config.useSingleServer()
                .setAddress("redis://ayaka520:6379")
                .setPassword("gangajiang521");

        //通过Redisson.create(config) 指定配置文件 创建RedissonClient
        return Redisson.create(config);
    }

    //@Bean
    public RedissonClient redissonClient2() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://ayaka521:6380");
        return Redisson.create(config);
    }

    //@Bean
    public RedissonClient redissonClient3() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://ayaka521:6381");
        return Redisson.create(config);
    }


}
