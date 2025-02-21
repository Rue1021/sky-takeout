package com.sky.task;


import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务类，定时处理订单状态
 */
@Component //加上Component注解，表示这个类也需要实例化并交给Spring容器管理
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 每分钟检查是否有超时未支付订单
     */
    @Scheduled(cron = "00 * * * * ?")
    public void processTimeOutOrder() {
        log.info("超时未支付订单定时处理,{}", LocalDateTime.now());

        //检查是否有超时未支付订单
        LocalDateTime time = LocalDateTime.now().minusMinutes(15);

        List<Orders> list = orderMapper.getByStatusAndOrderTimeLessThan(Orders.PENDING_PAYMENT, time);

        if (list != null && list.size() > 0) {
            //如果有，则更新订单信息并取消订单
            for (Orders orders : list) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时未支付，自动取消订单");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 每日凌晨2点处理一直处于派送中的订单
     */
    @Scheduled(cron = "00 00 02 * * ?")
    public void processOrderInDelivery() {
        log.info("定时处理一直处于派送中的订单, {}", LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().minusMinutes(120);
        //检查是否有一直处于派送中的订单
        List<Orders> list = orderMapper.getByStatusAndOrderTimeLessThan(Orders.DELIVERY_IN_PROGRESS, time);

        if (list != null && list.size() > 0) {
            //如果有，则更新订单信息，设置为已完成
            for (Orders orders : list) {
                orders.setStatus(Orders.COMPLETED);
                orders.setDeliveryTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }

    }
}
