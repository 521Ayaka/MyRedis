package com.hmdp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

//暴露动态代理
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.hmdp.mapper")
@SpringBootApplication
public class FeedApp {

    public static void main(String[] args) {
        System.out.println("神里绫华的狗");
        SpringApplication.run(FeedApp.class, args);
    }

}


/**
 *
 *
 *
 * 2022/09/29 03:22:27 [error] 8479#8479:
 * *1 open() "/usr/share/nginx/html/hmdp/js/axios.min.map" failed
 * (2: No such file or directory),
 * client: 127.0.0.1, server: localhost,
 * request: "GET /js/axios.min.map HTTP/1.1",
 * host: "localhost:8080"
 */