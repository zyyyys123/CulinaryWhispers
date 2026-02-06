package com.zyyyys.culinarywhispers.module.commerce.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.common.utils.SecurityUtil;
import com.zyyyys.culinarywhispers.module.commerce.entity.Product;
import com.zyyyys.culinarywhispers.module.commerce.service.OrderService;
import com.zyyyys.culinarywhispers.module.commerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 交易电商控制器
 * @author zyyyys
 */
@RestController
@RequestMapping("/api/commerce")
@RequiredArgsConstructor
public class CommerceController {

    private final ProductService productService;
    private final OrderService orderService;

    // ================== 商品接口 ==================

    /**
     * 商品列表
     * @param keyword 关键词
     * @param categoryId 分类ID
     */
    @GetMapping("/product/list")
    public Result<Page<Product>> listProducts(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer categoryId,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return Result.success(productService.listProducts(keyword, categoryId, page, size));
    }

    /**
     * 商品详情
     * @param productId 商品ID
     */
    @GetMapping("/product/{productId}")
    public Result<Product> getProduct(@PathVariable Long productId) {
        return Result.success(productService.getById(productId));
    }

    // ================== 订单接口 ==================

    /**
     * 创建订单
     * @param productCounts 商品ID与数量映射 {productId: count}
     */
    @PostMapping("/order/create")
    public Result<Long> createOrder(@RequestBody Map<Long, Integer> productCounts) {
        Long userId = SecurityUtil.getUserId();
        return Result.success(orderService.createOrder(userId, productCounts));
    }

    /**
     * 取消订单
     * @param orderId 订单ID
     */
    @PostMapping("/order/{orderId}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long orderId) {
        Long userId = SecurityUtil.getUserId();
        orderService.cancelOrder(userId, orderId);
        return Result.success();
    }

    /**
     * 支付回调 (模拟)
     * @param orderId 订单ID
     */
    @PostMapping("/payment/notify")
    public Result<Void> paymentNotify(@RequestParam Long orderId) {
        orderService.paySuccess(orderId);
        return Result.success();
    }
}
