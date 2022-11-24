package com.ganga;

import com.ganga.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = RedisIdWorkerApp.class)
class ShopNewCacheTests {

    @Autowired
    private ShopServiceImpl shopServiceImpl;

    @Test
    void shopCacheTest() throws InterruptedException {


    }


}
