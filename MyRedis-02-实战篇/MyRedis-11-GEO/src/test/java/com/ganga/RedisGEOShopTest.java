package com.ganga;

import com.ganga.entity.Shop;
import com.ganga.service.IShopService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ganga.utils.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
@ContextConfiguration(classes = GEOApp.class)
public class RedisGEOShopTest {

    @Resource
    private IShopService shopService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询所有商铺 将 [店铺类型] [id] [经纬度] 存入Redis当中
     * 数据类型: GEO
     * key:     店铺类型
     * member:  店铺id
     * score:   经纬度
     */
    @Test
    void addEGORedisShop(){
        //1.获取所有店铺
        List<Shop> shopList = shopService.list();
        //2.根据店铺类型进行分组
        Map<Long, List<Shop>> collect = shopList.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        //3.添加到 Redis 当中
        collect.forEach((typeId, value) -> {
            //3.1.获取店铺类型id key
            String key = SHOP_GEO_KEY + typeId.toString();
            //3.2.同种店铺封装到集合当中 [店铺] + [经纬坐标] 封装成 RedisGeoCommands.GeoLocation<String>>
            ArrayList<RedisGeoCommands.GeoLocation<String>> location = new ArrayList<>();
            value.forEach(shop -> location.add(new RedisGeoCommands.GeoLocation<>(shop.getId().toString(), new Point(shop.getX(), shop.getY()))));
            //3.3.写入 Redis 当中
            stringRedisTemplate.opsForGeo().add(key, location);
        });
    }

}


