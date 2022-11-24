package com.hmdp.tools;

import com.hmdp.entity.VoucherOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class BlockingQueueTest {

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
                    //handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.debug("订单任务失败", e);
                }
            }
        }
    }

}
