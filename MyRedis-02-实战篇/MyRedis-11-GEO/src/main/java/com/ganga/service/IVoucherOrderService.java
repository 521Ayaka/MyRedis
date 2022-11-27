package com.ganga.service;

import com.ganga.dto.Result;
import com.ganga.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

    void queryOrderVoucherSave(VoucherOrder voucherOrder);
}
