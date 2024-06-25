package com.review.service;

import com.review.entity.VoucherOrder;

public interface OrderMessageHandler {

    void processOrderMessage(VoucherOrder voucherOrder);


}
