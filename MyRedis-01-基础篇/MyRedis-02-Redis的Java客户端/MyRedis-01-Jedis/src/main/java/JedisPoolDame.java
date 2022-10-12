import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

//配置连接池
public class JedisPoolDame {

    private static final JedisPool jedisPool;

    static{
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        //设置最大连接数量
        jedisPoolConfig.setMaxTotal(8);
        //设置最大空闲连接数量
        jedisPoolConfig.setMaxIdle(8);
        //设置最小空闲连接数量
        jedisPoolConfig.setMinIdle(0);
        //设置最长等待时间 ms
        jedisPoolConfig.setMaxWaitMillis(200);
        jedisPool = new JedisPool(
                jedisPoolConfig,"ayaka520",6379,1000,"gangajiang521"
        );
    }

    //获取Jedis对象
    public static Jedis getJedis(){
        return jedisPool.getResource();
    }
}
