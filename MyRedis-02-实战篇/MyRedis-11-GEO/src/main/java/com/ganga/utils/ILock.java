package com.ganga.utils;


/**
 * 基于Redis的分布式锁
 */
public interface ILock {

    /**
     * 尝试获取锁
     * @param timeoutSec 兜底过期时间
     * @return 获取是否成功 true成功
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unLock();

}
