package com.sky.task;

import com.sky.constant.Orderstatus;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class Mytask {
@Autowired
    OrderMapper orderMapper;
    @Scheduled(cron = "0 * * * * ?")
    public void  task() {
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(15);
        List<Orders> orders = orderMapper.selectovertime(Orderstatus.PENDING_PAYMENT, localDateTime);
        log.info("超时订单有{}单", orders.size());
        if (orders != null && orders.size() > 0) {
            for (Orders order : orders) {
                order.setStatus(Orderstatus.CANCELLED);
                order.setCancelReason("订单超时，自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.updateById(order);

            }

        }
    }
        @Scheduled(cron = "0 0 1 * * ?") //每天凌晨1点触发一次
        public void processDeliveryOrder () {
            log.info("定时处理处于派送中的订单：{}", LocalDateTime.now());

            LocalDateTime time = LocalDateTime.now().plusMinutes(-60);

            List<Orders> ordersList = orderMapper.selectovertime(Orderstatus.DELIVERY_IN_PROGRESS, time);

            if (ordersList != null && ordersList.size() > 0) {
                for (Orders orders : ordersList) {
                    orders.setStatus(Orderstatus.COMPLETED);
                    orderMapper.updateById(orders);
                }
            }
        }


    }

