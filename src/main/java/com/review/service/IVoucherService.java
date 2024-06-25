package com.review.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.review.dto.Result;
import com.review.entity.Voucher;

public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);
}
