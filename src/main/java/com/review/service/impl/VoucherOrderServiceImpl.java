package com.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.review.dto.Result;
import com.review.dto.UserDTO;
import com.review.entity.SeckillVoucher;
import com.review.entity.VoucherOrder;
import com.review.mapper.VoucherOrderMapper;
import com.review.service.ISeckillVoucherService;
import com.review.service.IVoucherOrderService;
import com.review.utils.RedisIdWorker;
import com.review.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;

import static com.review.config.RabbitMQConfig.ORDER_QUEUE;

@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RabbitTemplate rabbitTemplate;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /**
     * 秒杀优惠券(消息队列)
     *
     * @param voucherId 券id
     * @return {@link Result}
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        //获取用户
        UserDTO user = UserHolder.getUser();
        //获取订单id
        Long orderId = redisIdWorker.nextId("order");
        //执行lua脚本
        Long res = stringRedisTemplate.execute(
                SECKILL_SCRIPT
                , Collections.emptyList()
                , voucherId.toString()
                , user.getId().toString()
                , orderId.toString());
        //判断结果是否为0
        int r = res.intValue();
        if (r != 0) {
            //不为0 没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "禁止重复下单");
        }

        // 订单基本信息发送到消息队列
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setUserId(user.getId());
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setId(orderId);

        boolean messageSent = false;
        int retryCount = 0;
        int maxRetries = 3;

        while (!messageSent && retryCount < maxRetries) {
            try {
                rabbitTemplate.convertAndSend(ORDER_QUEUE, voucherOrder);
                messageSent = true;
            } catch (Exception e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    // 如果多次重试仍然失败，记录日志或其他处理
                    return Result.fail("消息发送失败，订单无法完成");
                }
            }
        }

        return Result.ok(orderId);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        //扣减库存
        boolean isSuccess = seckillVoucherService.update(
                new LambdaUpdateWrapper<SeckillVoucher>()
                        .eq(SeckillVoucher::getVoucherId, voucherOrder.getVoucherId())
                        .gt(SeckillVoucher::getStock, 0)
                        .setSql("stock=stock-1"));
        //创建订单
        this.save(voucherOrder);
    }
}
