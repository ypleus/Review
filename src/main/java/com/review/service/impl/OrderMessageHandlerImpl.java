package com.review.service.impl;

import com.review.entity.VoucherOrder;
import com.review.service.OrderMessageHandler;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class OrderMessageHandlerImpl implements OrderMessageHandler {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private VoucherOrderServiceImpl voucherOrderServiceImpl;

    private static final int MAX_RETRY_COUNT = 3;

    // 创建订单
    @Override
    public void processOrderMessage(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        Long orderId = voucherOrder.getId();

        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock();
        if (!isLock) {
            throw new RuntimeException("发送未知错误");
        }
        try {
            Integer failCount = (Integer) stringRedisTemplate.opsForHash().get("order:failCount", orderId.toString());
            if (failCount == null) {
                failCount = 0;
            }
            try {
                voucherOrderServiceImpl.createVoucherOrder(voucherOrder);
                stringRedisTemplate.opsForHash().delete("order:failCount", orderId.toString());
            } catch (Exception e) {
                failCount++;
                stringRedisTemplate.opsForHash().put("order:failCount", orderId, failCount);
                if (failCount >= MAX_RETRY_COUNT) {
                    rabbitTemplate.convertAndSend("deadLetterQueue", voucherOrder);
                    stringRedisTemplate.opsForHash().delete("order:failCount", orderId);
                }
                throw e;
            }
        } finally {
            lock.unlock();
        }
    }

}