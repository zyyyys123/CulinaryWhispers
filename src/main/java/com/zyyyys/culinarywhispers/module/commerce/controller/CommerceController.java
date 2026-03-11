package com.zyyyys.culinarywhispers.module.commerce.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.common.utils.SecurityUtil;
import com.zyyyys.culinarywhispers.module.commerce.dto.CreateOrderRequest;
import com.zyyyys.culinarywhispers.module.commerce.entity.Product;
import com.zyyyys.culinarywhispers.module.commerce.service.OrderService;
import com.zyyyys.culinarywhispers.module.commerce.service.ProductService;
import com.zyyyys.culinarywhispers.module.commerce.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 交易电商控制器
 * @author zyyyys
 */
@RestController
@RequestMapping("/api/commerce")
@RequiredArgsConstructor
@Tag(name = "市集", description = "市集商品与订单（模拟支付）")
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
    @Operation(summary = "商品列表", description = "分页获取商品列表，支持关键词与分类筛选")
    public Result<Page<Product>> listProducts(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer categoryId,
                                              @Parameter(description = "页码（从 1 开始）", example = "1")
                                              @RequestParam(defaultValue = "1") int page,
                                              @Parameter(description = "每页数量", example = "10")
                                              @RequestParam(defaultValue = "10") int size) {
        return Result.success(productService.listProducts(keyword, categoryId, page, size));
    }

    /**
     * 商品详情
     * @param productId 商品ID
     */
    @GetMapping("/product/{productId}")
    @Operation(summary = "商品详情", description = "获取指定商品详情")
    public Result<Product> getProduct(@Parameter(description = "商品ID", example = "1")
                                      @PathVariable Long productId) {
        return Result.success(productService.getById(productId));
    }

    // ================== 订单接口 ==================

    /**
     * 创建订单
     * @param productCounts 商品ID与数量映射 {productId: count}
     */
    @PostMapping("/order/create")
    @Operation(
            summary = "创建订单",
            description = "根据商品ID与数量创建订单，返回订单ID",
            requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CreateOrderRequest.class)))
    )
    public Result<Long> createOrder(@org.springframework.web.bind.annotation.RequestBody CreateOrderRequest request) {
        Long userId = SecurityUtil.getUserId();
        return Result.success(orderService.createOrder(userId, request.getProductCounts()));
    }

    @GetMapping("/order/list")
    @Operation(summary = "订单列表", description = "分页获取当前登录用户的订单列表，可按状态筛选")
    public Result<Page<OrderVO>> listMyOrders(@Parameter(description = "页码（从 1 开始）", example = "1")
                                              @RequestParam(defaultValue = "1") int page,
                                              @Parameter(description = "每页数量", example = "10")
                                              @RequestParam(defaultValue = "10") int size,
                                              @Parameter(description = "订单状态（可选）", example = "1")
                                              @RequestParam(required = false) Integer status) {
        Long userId = SecurityUtil.getUserId();
        return Result.success(orderService.pageMyOrders(userId, page, size, status));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "订单详情", description = "获取指定订单详情（仅允许查看自己的订单）")
    public Result<OrderVO> getMyOrder(@Parameter(description = "订单ID", example = "10001")
                                      @PathVariable Long orderId) {
        Long userId = SecurityUtil.getUserId();
        return Result.success(orderService.getMyOrder(userId, orderId));
    }

    /**
     * 取消订单
     * @param orderId 订单ID
     */
    @PostMapping("/order/{orderId}/cancel")
    @Operation(summary = "取消订单", description = "取消指定订单（仅允许取消自己的订单）")
    public Result<Void> cancelOrder(@Parameter(description = "订单ID", example = "10001")
                                    @PathVariable Long orderId) {
        Long userId = SecurityUtil.getUserId();
        orderService.cancelOrder(userId, orderId);
        return Result.success();
    }

    @PostMapping("/order/{orderId}/deliver")
    @Operation(summary = "发货（模拟）", description = "将订单状态置为已发货（模拟，仅允许操作自己的订单）")
    public Result<Void> deliver(@Parameter(description = "订单ID", example = "10001")
                                @PathVariable Long orderId) {
        Long userId = SecurityUtil.getUserId();
        orderService.deliverOrder(userId, orderId);
        return Result.success();
    }

    @PostMapping("/order/{orderId}/finish")
    @Operation(summary = "确认收货/完成（模拟）", description = "将订单状态置为已完成（模拟，仅允许操作自己的订单）")
    public Result<Void> finish(@Parameter(description = "订单ID", example = "10001")
                               @PathVariable Long orderId) {
        Long userId = SecurityUtil.getUserId();
        orderService.finishOrder(userId, orderId);
        return Result.success();
    }

    /**
     * 支付回调 (模拟)
     * @param orderId 订单ID
     */
    @PostMapping("/payment/notify")
    @Operation(summary = "支付回调（模拟）", description = "模拟支付成功回调，将订单状态置为已支付")
    public Result<Void> paymentNotify(@Parameter(description = "订单ID", example = "10001")
                                      @RequestParam Long orderId) {
        orderService.paySuccess(orderId);
        return Result.success();
    }
}
