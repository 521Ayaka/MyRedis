import redis.clients.jedis.Jedis;

public class JedisPoolGet {

    public static void main(String[] args) {

        Jedis jedis = JedisPoolDame.getJedis();

        String resort = jedis.set("尴尬", "这就尴尬了");
        System.out.println("resort = " + resort);

        String get = jedis.get("尴尬");
        System.out.println("get = " + get);

        jedis.close();

    }

}
