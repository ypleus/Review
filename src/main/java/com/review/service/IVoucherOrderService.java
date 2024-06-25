package com.review.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.review.dto.Result;
import com.review.entity.VoucherOrder;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;

public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 秒杀优惠券
     *
     * @param voucherId 券id
     * @return {@link Result}
     */
    Result seckillVoucher(Long voucherId);


    /**
     * 创建优惠券订单
     *
     * @param voucherOrder 券订单
     */
    @NotNull
    @Transactional(rollbackFor = Exception.class)
    void createVoucherOrder(VoucherOrder voucherOrder);
}
