# Redis 相关命令





## 关于安装

### Redis安装

Redis是基于C语言编写的，因此首先需要安装Redis所需要的gcc依赖：

```sh
yum install -y gcc tcl
```

解压缩：

压缩在 `/usr/local/src`下

```sh
tar -xzf redis-6.2.6.tar.gz
```

解压后：

进入redis目录：

```sh
cd redis-6.2.6
```

运行编译命令：

```sh
make && make install
```

如果没有出错，应该就安装成功了。

默认的安装路径是在 `/usr/local/bin`目录下：

该目录以及默认配置到环境变量，因此可以在任意目录下运行这些命令。其中：

- redis-cli：是redis提供的命令行客户端
- redis-server：是redis的服务端启动脚本
- redis-sentinel：是redis的哨兵启动脚本

---

**如果没有任意目录下启动**

修改profile文件： 

```bash
vi /etc/profile 
```


在最后行添加: 

```bash
export PATH=$PATH:/usr/local/bin
```

注意：/usr/local/bin表示的是redis-server 命令存在的目录路径

重新加载/etc/profile 

```bash
source /etc/profile  
```


在任意目录执行命令

```bash
redis-server 
```



---

### **Redis配置文件**

我们先将这个配置文件备份一份：

```
cp redis.conf redis.conf.bck
```



然后修改redis.conf文件中的一些配置：

```properties
# 允许访问的地址，默认是127.0.0.1，会导致只能在本地访问。修改为0.0.0.0则可以在任意IP访问，生产环境不要设置为0.0.0.0
bind 0.0.0.0
# 守护进程，修改为yes后即可后台运行
daemonize yes 
# 密码，设置后访问Redis必须输入密码
requirepass 123321
```



Redis的其它常见配置：

```properties
# 监听的端口
port 6379
# 工作目录，默认是当前目录，也就是运行redis-server时的命令，日志、持久化等文件会保存在这个目录
dir .
# 数据库数量，设置为1，代表只使用1个库，默认有16个库，编号0~15
databases 1
# 设置redis能够使用的最大内存
maxmemory 512mb
# 日志文件，默认为空，不记录日志，可以指定日志文件名
logfile "redis.log"
```



启动Redis：

```sh
# 进入redis安装目录 
cd /usr/local/src/redis-6.2.6
# 启动
redis-server redis.conf
```



停止服务：

```sh
# 利用redis-cli来执行 shutdown 命令，即可停止 Redis 服务，
# 因为之前配置了密码，因此需要通过 -u 来指定密码
redis-cli -u 123321 shutdown
```





---

### 设置自启动

我们也可以通过配置来实现开机自启。

首先，新建一个系统服务文件：

```sh
vi /etc/systemd/system/redis.service
```

内容如下：

```conf
[Unit]
Description=redis-server
After=network.target

[Service]
Type=forking
ExecStart=/usr/local/bin/redis-server /usr/local/src/redis-6.2.6/redis.conf
PrivateTmp=true

[Install]
WantedBy=multi-user.target
```



然后重载系统服务：

```sh
systemctl daemon-reload
```



现在，我们可以用下面这组命令来操作redis了：

```sh
# 启动
systemctl start redis
# 停止
systemctl stop redis
# 重启
systemctl restart redis
# 查看状态
systemctl status redis
```



执行下面的命令，可以让redis开机自启：

```sh
systemctl enable redis
```



---

### 进入客户端

Redis安装完成后就自带了命令行客户端：redis-cli，使用方式如下：

```sh
redis-cli [options] [commonds]
```

其中常见的options有：

- `-h 127.0.0.1`：指定要连接的redis节点的IP地址，默认是127.0.0.1
- `-p 6379`：指定要连接的redis节点的端口，默认是6379
- `-a 123321`：指定redis的访问密码 

其中的commonds就是Redis的操作命令，例如：

- `ping`：与redis服务端做心跳测试，服务端正常会返回`pong`

不指定commond时，会进入`redis-cli`的交互控制台：



还可以这样：

```bash
[root@gangajiang ~]# redis-cli -h 127.0.0.1 -p 6379
127.0.0.1:6379> auth 密码
OK
127.0.0.1:6379> ping
PONG
```



---





## 通用命令

Redis通用命令

help + 关键字  ：查看该关键字用法

通用指令是部分数据类型的，都可以使用的指令，常见的有： 

- KEYS：查看符合模板的所有key 
  - *全部  / 占多个字符
  -  ?占一个字符

- DEL：删除一个指定的key 

- EXISTS：判断key是否存在 

- EXPIRE：给一个key设置有效期，有效期到期时该key会被自动删除 

- TTL：查看一个KEY的剩余有效期

- RENAME: 对一个key重命名



---



## String类型的常见命令

String类型，也就是字符串类型，是Redis中最简单的存储类型。 其value是字符串，不过根据字符串的格式不同，又可以分为3类： 

- string：普通字符串 
- int：整数类型，可以做自增、自减操作
- float：浮点类型，可以做自增、自减操作



String的常见命令有： 

- SET：添加或者修改已经存在的一个String类型的键值对 
- GET：根据key获取String类型的value
- MSET：批量添加多个String类型的键值对
- MGET：根据多个key获取多个String类型的value
- INCR：让一个整型的key自增1
- INCRBY: 让一个整型的key自增并指定步长，例如：incrby num 2 让num值自增2 
- INCRBYFLOAT：让一个浮点类型的数字自增并指定步长 
- SETNX：添加一个String类型的键值对，前提是这个key不存在，否则不执行 
- SETEX：添加一个String类型的键值对，并且指定有效期



---



## key的结构





Redis的key允许有多个单词形成层级结构，多个单词之间用':'隔开，格式如下：

 **项目名:业务名:类型:id**



 这个格式并非固定，也可以根据自己的需求来删除或添加词条。 

例如我们的项目名称叫 heima，有user和product两种不同类型的数据，我们可以这样定义key： 

◆ user相关的key：heima:user:1 

◆ product相关的key：heima:product:1 



如果Value是一个Java对象，例如一个User对象，则可以将对象序列化为JSON字符串后存储：



| KEY             | VALUE                                     |
| --------------- | ----------------------------------------- |
| ganga:user:1    | {"id":1, "name": "Jack", "age": 21}       |
| ganga:product:1 | {"id":1, "name": "小米11", "price": 4999} |

```
set ganga:user:1 '{"id":1, "name": "Jack", "age": 21}'
set ganga:product:1 '{"id":1, "name": "小米11", "price": 4999}'

```





| KEY             | VALUE                                   |
| --------------- | --------------------------------------- |
| ganga:user:2    | {"id":2, "name":"Rose", "age": 18}      |
| ganga:product:2 | {"id":2, "name":"荣耀6", "price": 2999} |

```
set ganga:user:2 '{"id":2, "name":"Rose", "age": 18}'
set ganga:product:2 '{"id":2, "name":"荣耀6", "price": 2999}'
```





---

## Hash类型



Hash类型，也叫散列，其value是一个无序字典，类似于Java中的HashMap结构。 

String结构是将对象序列化为JSON字符串后存储，当需要修改对象某个字段时很不方便：

Hash结构可以将对象中的每个字段独立存储，可以针对单个字段做CRUD： 



Hash的常见命令有： 

- HSET key field value：添加或者修改hash类型key的field的值 
- HGET key field：获取一个hash类型key的field的值 
- HMSET：批量添加多个hash类型key的field的值 
- HMGET：批量获取多个hash类型key的field的值 
- HGETALL：获取一个hash类型的key中的所有的field和value 
- HKEYS：获取一个hash类型的key中的所有的field 
- HVALS：获取一个hash类型的key中的所有的value 
- HINCRBY:让一个hash类型key的字段值自增并指定步长 
- HSETNX：添加一个hash类型的key的field值，前提是这个field不存在，否则不执行





---



## List类型



Redis中的List类型与Java中的LinkedList类似，可以看做是一个双向链表结构。

既可以支持正向检索和也可以支持反 向检索。 

特征也与LinkedList类似： 

- 有序 

- 元素可以重复 

- 插入和删除快 

- 查询速度一般

常用来存储一个有序数据，例如：朋友圈点赞列表，评论列表等。



**List类型的常见命令**

List的常见命令有： 

- LPUSH key element ... ：向列表左侧插入一个或多个元素 
- LPOP key：移除并返回列表左侧的第一个元素，没有则返回nil  // 第一个元素是一
- RPUSH key element ... ：向列表右侧插入一个或多个元素 
- RPOP key：移除并返回列表右侧的第一个元素 
- LRANGE key star end：返回一段角标范围内的所有元素          l   // 第一个范围是零
- BLPOP和BRPOP：与LPOP和RPOP类似，只不过在没有元素时等待指定时间，而不是直接返回nil







---

 

## Set类型



Redis的Set结构与Java中的HashSet类似，可以看做是一个value为null的HashMap。

因为也是一个hash表，因此具 备与HashSet类似的特征： 

- 无序 
- 元素不可重复 
- 查找快 
- 支持交集、并集、差集等功能





**Set类型的常见命令**

String的常见命令有： 

- SADD key member ... ：向set中添加一个或多个元素  
- SREM key member ... : 移除set中的指定元素 
- SCARD key： 返回set中元素的个数 
- SISMEMBER key member：判断一个元素是否存在于set中 
- SMEMBERS：获取set中的所有元素 
- SINTER key1 key2 ... ：求key1与key2的交集
- SDIFF key1 key2 ... ：求key1与key2的差集 
- SUNION key1 key2 ..：求key1和key2的并集



---

---





## SortedSet类型

Redis的SortedSet是一个可排序的set集合，与Java中的TreeSet有些类似，但底层数据结构却差别很大。SortedSet 中的每一个元素都带有一个score属性，可以基于score属性对元素排序，底层的实现是一个**跳表（SkipList）加 hash表**。

* *SortedSet具备下列特性：** 

- 可排序 
- 元素不重复 
- 查询速度快 

因为SortedSet的可排序特性，**经常被用来实现排行榜这样的功能。**



---



**SortedSet类型的常见命令**

SortedSet的常见命令有： 

- ZADD key score member：添加一个或多个元素到sorted set ，如果已经存在则更新其score值 
- ZREM key member：删除sorted set中的一个指定元素 
- ZSCORE key member : 获取sorted set中的指定元素的score值 
- ZRANK key member：获取sorted set 中的指定元素的排名 
- ZCARD key：获取sorted set中的元素个数 
- ZCOUNT key min max：统计score值在给定范围内的所有元素的个数 
- ZINCRBY key increment member：让sorted set中的指定元素自增，步长为指定的increment值 
- ZRANGE key min max：按照score排序后，获取指定排名范围内的元素 
- ZRANGEBYSCORE key min max：按照score排序后，获取指定score范围内的元素 
- ZDIFF、ZINTER、ZUNION：求差集、交集、并集 注意：所有的排名默认都是升序，如果要降序则在命令的Z后面添加REV即可





---







# Redis 实现短信登陆功能



## pom.xml  依赖文件

![pom](F:\AppCode\MyCode\MyRedis\MyRedis-01-基础篇\redis.resources\pom.png)



```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-pool2</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <scope>runtime</scope>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
        <version>3.4.3</version>
    </dependency>

    <!--hutool-->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.5</version>
    </dependency>
</dependencies>
```



## 实体类文件

User.java







































