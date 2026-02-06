package com.zyyyys.culinarywhispers.module.commerce.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zyyyys.culinarywhispers.module.commerce.entity.Order;

import java.util.Map;

/**
 * 订单服务接口
 * @author zyyyys
 */
public interface OrderService extends IService<Order> {

    /**
     * 创建订单
     * @param userId 用户ID
     * @param productCounts 商品ID与数量映射
     * @return 订单ID
     */
    Long createOrder(Long userId, Map<Long, Integer> productCounts);

    /**
     * 取消订单
     * @param userId 用户ID (鉴权)
     * @param orderId 订单ID
     */
    void cancelOrder(Long userId, Long orderId);

    /**
     * 支付回调 (模拟)
     * @param orderId 订单ID
     */
    void paySuccess(Long orderId);
}
