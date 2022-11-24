package com.ganga.service.impl;

import com.ganga.dto.Result;
import com.ganga.entity.SeckillVoucher;
import com.ganga.entity.VoucherOrder;
import com.ganga.mapper.VoucherOrderMapper;
import com.ganga.service.ISeckillVoucherService;
import com.ganga.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ganga.utils.RedisIdWorker;
import com.ganga.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;


/**
 * <p>
 * 服务实现类
 * </p>
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService iSeckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    //@Resource
    //private StringRedisTemplate stringRedisTemplate;

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
        /*synchronized (userId.toString().intern()) {
            //获取当前代理对象
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            //通过代理对象调用 保证事务的正常
            return proxy.queryOrderVoucherSave(voucherId);
        }*/

        //获取分布式锁对象
        //LockImpl lock = new LockImpl(stringRedisTemplate,"order:" + userId);

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


    }


    /**
     *<br> 几个重点：
     *<br> 1. 一人一单问题使用 悲观锁 还是 乐观锁？
     *<br>      使用悲观锁
     *<br> 2. 使用悲观锁 synchronized 加载 方法上 还是 内部？
     *<br>      内部，如果加载方法上，那整个订单业务都是串行，那刚刚解决的 超卖问题[乐观锁]也没意义了
     *<br> 3. synchronized 的锁对象是什么
     *<br>      userId.toString().intern()
     *<br>          而不是 userId.toString()
     *<br>          Long的 toString() 底层是：
     *<br>          return new String(buf, UTF16);
     *<br>          .intern()方法是：返回字符串对象的规范表示。
     *<br> 4. 对于事务的添加 是 锁释放完了再提交 还是 提交完了再释放锁
     *<br>      提交完了再释放锁
     *<br>      具体操作：
     *<br>          1.在方法上加上@Transactional
     *<br>          2.在调用者 调用语句外加上 synchronized
     *<br>             synchronized (userId.toString().intern()) {
     *<br>                 return queryOrderVoucherSave(voucherId);
     *<br>             }
     *<br> 5. 事务失效问题
     *<br>      spring的事务是 AOP动态代理的
     *<br>          this.queryOrderVoucherSave(voucherId) //并非是代理对象
     *<br>      解决方法
     *<br>          这里使用 获取动态代理的方式 :
     *<br>              //获取动态代理对象
     *<br>              IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
     *<br>              //通过代理对象调用
     *<br>              proxy.queryOrderVoucherSave(voucherId);
     *<br>          注意: 使用AopContext.currentProxy()
     *<br>                  导入aspectjweaver依赖
     *<br>                  开启 @EnableAspectJAutoProxy(exposeProxy = true) 暴露代理对象
     *<br>
     *<br> @param voucherId
     *<br> @return
     */
    @Transactional
    public Result queryOrderVoucherSave(Long voucherId) {

        //获取用户id
        Long userId = UserHolder.getUser().getId();

        //判断订单表中是否已经存在
        int count = query()
                .eq("user_id", userId)
                .eq("voucher_id", voucherId)
                .count();
        if (count > 0) {
            //存在，返回前端信息
            return Result.fail("你已领取，每人只能领取一份！");
        }

        //以上都满足 施行扣减下单业务
        //5.扣减库存
        boolean isOK = iSeckillVoucherService
                .update()
                .setSql("stock =stock - 1")
                .eq("voucher_id", voucherId)
                //.eq("stock",stock) // CAS乐观锁
                // CAS乐观锁改进  stock > 0 就可以执行下单业务
                .gt("stock", 0)
                .update();
        if (!isOK) {
            //秒杀失败，返回给前端异常信息
            return Result.fail("库存不足！");
        }
        //6.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //6.1.生成订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        //6.2.设置用户id
        voucherOrder.setUserId(userId);
        //6.3.设置代金卷id
        voucherOrder.setVoucherId(voucherId);
        //6.4.当生产的订单id写入数据库
        this.save(voucherOrder);

        //7.返回订单ID
        return Result.ok(orderId);

    }


}
