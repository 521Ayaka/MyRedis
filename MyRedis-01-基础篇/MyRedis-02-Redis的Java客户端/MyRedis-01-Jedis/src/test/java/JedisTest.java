import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;

public class JedisTest {

    //Jedis客户端对象
    private Jedis jedis;

    //前置：设置连接信息
    @Before
    public void connect(){
        //连接源
        jedis = new Jedis("ayaka520",6379);
        //认证密码
        jedis.auth("gangajiang521");
        //选择库
        jedis.select(0);
    }

    //测试
    @Test
    public void testString(){

        //新增数据
        String result = jedis.set("尴尬", "ganga");
        System.out.println("result = " + result);

        //获取数据
        String value = jedis.get("尴尬");
        System.out.println("get = " + value);
    }

    //后置 释放资源
    @After
    public void close(){
        if (jedis != null){
            jedis.close();
        }
    }


}
