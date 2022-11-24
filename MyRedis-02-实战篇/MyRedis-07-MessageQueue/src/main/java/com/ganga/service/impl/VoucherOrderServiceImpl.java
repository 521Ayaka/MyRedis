package com.ganga.service.impl;

import com.ganga.dto.Result;
import com.ganga.entity.VoucherOrder;
import com.ganga.mapper.VoucherOrderMapper;
import com.ganga.service.ISeckillVoucherService;
import com.ganga.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ganga.utils.RedisIdWorker;
import com.ganga.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * <p>
 * 服务实现类
 * </p>
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService iSeckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    //当前代理对象
    private IVoucherOrderService proxy;

    //购买资格判断的 Lua 脚本
    private static final DefaultRedisScript<Long> SECKILL_LUA;
    static {
        SECKILL_LUA = new DefaultRedisScript<>();
        SECKILL_LUA.setResultType(Long.class);
        SECKILL_LUA.setLocation(new ClassPathResource("seckill.lua"));
    }

    //阻塞队列
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024*1024);

    //线程池
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    //执行阻塞队列
    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandle());
    }
    //处理订单的异步任务
    private class VoucherOrderHandle implements Runnable{
        @Override
        public void run() {
            //等待任务
            while(true){
                try {
                    //从阻塞队列中取
                    VoucherOrder voucherOrder = orderTasks.take();
                    //处理订单
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.debug("订单任务失败", e);
                }
            }
        }
    }

    /**
     * 处理写入新订单的任务
     * @param
     */
    private void handleVoucherOrder(VoucherOrder voucherOrder) {

        Long userId = voucherOrder.getId();

        RLock lock = redissonClient1.getLock("lock:order:" + userId);

        //尝试获取锁
        boolean isLock = lock.tryLock();

        //判断是否获取成功
        if (!isLock){
            //获取锁失败
            log.error("不能重复下单！");
            return;
        }

        //成功 执行业务
        try{
            //通过代理对象调用 保证事务的正常
            proxy.queryOrderVoucherSave(voucherOrder);
        }finally{
            //确保锁的释放
            lock.unlock();
        }

    }

    /**
     * Redisson
     */
    @Resource
    private RedissonClient redissonClient1;

    /**
     * 实现订单秒杀业务
     *
     * @param voucherId
     * @return
     */
    @Override
    public Result seckillVoucher(Long voucherId) {

        //获取用户 id
        Long userId = UserHolder.getUser().getId();
        //执行 Lua 脚本
        Long noQualified = stringRedisTemplate.execute(SECKILL_LUA, Collections.emptyList(), voucherId, userId);
        //判断返回结果是否为 0
        if (noQualified != 0){
            // --返回 1 表示库存不足 -- 返回 2 表示不能重复下单
            return Result.fail( noQualified == 1 ? "库存不足!" : "不能重复下单！" ) ;
        }

        //生成订单信息
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(userId);

        //获取当前代理对象
        this.proxy = (IVoucherOrderService) AopContext.currentProxy();

        //将优惠卷id 用户id 订单id 存入到阻塞队列当中
        orderTasks.add(voucherOrder);

        //返回订单id
        return Result.ok(orderId);
    }


    /*@Override
    public Result seckillVoucher(Long voucherId) {
        //1.查询优惠卷信息
        SeckillVoucher voucher = iSeckillVoucherService.getById(voucherId);
        Integer stock = voucher.getStock();
        //2.判断优惠卷是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            //尚未开始，返回异常给前端
            return Result.fail("秒杀尚未开始！");
        }
        //3.判断优惠卷是否过期
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            //秒杀已结束，返回前端异常信息
            return Result.fail("秒杀已结束！");
        }
        //4.判断优惠卷库存是否充足
        if (stock < 1) {
            //秒杀卷库存不足，返回给前端异常信息
            return Result.fail("库存不足！");
        }


        //一人一单问题
        //获取用户id
        Long userId = UserHolder.getUser().getId();

        //缩小悲观锁范围
        //获取分布式锁对象
        //从Redisson中获取锁
        RLock lock = redissonClient1.getLock("lock:order:" + userId);

        //尝试获取锁
        boolean isLock = lock.tryLock();

        //判断是否获取成功
        if (!isLock){
            //获取锁失败
            return Result.fail("不能重复下单！");
        }
        //成功 执行业务
        try{
            //获取当前代理对象
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            //通过代理对象调用 保证事务的正常
            return proxy.queryOrderVoucherSave(voucherId);
        }finally{
            //确保锁的释放
            lock.unlock();
        }


    }*/

    @Override
    @Transactional
    public void queryOrderVoucherSave(VoucherOrder voucherOrder) {

        //获取用户id
        Long userId = UserHolder.getUser().getId();

        //判断订单表中是否已经存在
        int count = query()
                .eq("user_id", userId)
                .eq("voucher_id", voucherOrder.getVoucherId())
                .count();
        if (count > 0) {
            //存在
            log.error("不能重复下单！");
            return;
        }

        //以上都满足 施行扣减下单业务
        //5.扣减库存
        boolean isOK = iSeckillVoucherService
                .update()
                .setSql("stock =stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                //.eq("stock",stock) // CAS乐观锁
                // CAS乐观锁改进  stock > 0 就可以执行下单业务
                .gt("stock", 0)
                .update();
        if (!isOK) {
            //秒杀失败，返回异常信息
            log.error("库存不足！");
            return;
        }

        //写入数据库
        this.save(voucherOrder);

    }


}
