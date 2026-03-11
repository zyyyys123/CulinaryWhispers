package com.zyyyys.culinarywhispers.module.commerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import com.zyyyys.culinarywhispers.module.commerce.entity.Order;
import com.zyyyys.culinarywhispers.module.commerce.entity.OrderItem;
import com.zyyyys.culinarywhispers.module.commerce.entity.Product;
import com.zyyyys.culinarywhispers.module.commerce.mapper.OrderItemMapper;
import com.zyyyys.culinarywhispers.module.commerce.mapper.OrderMapper;
import com.zyyyys.culinarywhispers.module.commerce.service.OrderService;
import com.zyyyys.culinarywhispers.module.commerce.service.ProductService;
import com.zyyyys.culinarywhispers.module.commerce.vo.OrderItemVO;
import com.zyyyys.culinarywhispers.module.commerce.vo.OrderVO;
import com.zyyyys.culinarywhispers.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 * @author zyyyys
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final ProductService productService;
    private final OrderItemMapper orderItemMapper;
    private final UserService userService;

    /**
     * 创建订单
     * @param userId 用户ID
     * @param productCounts 商品ID与购买数量的映射
     * @return 订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(Long userId, Map<Long, Integer> productCounts) {
        if (productCounts == null || productCounts.isEmpty()) {
            throw new BusinessException(400, "订单商品不能为空");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        // 1. 扣减库存并计算金额
        for (Map.Entry<Long, Integer> entry : productCounts.entrySet()) {
            Long productId = entry.getKey();
            Integer count = entry.getValue();

            Product product = productService.getById(productId);
            if (product == null) {
                throw new BusinessException(ResultCode.DATA_NOT_FOUND.getCode(), "商品不存在: " + productId);
            }

            // 扣减库存
            boolean success = productService.reduceStock(productId, count);
            if (!success) {
                throw new BusinessException(400, "商品库存不足: " + product.getTitle());
            }

            // 计算金额
            BigDecimal itemTotal = product.getPrice().multiply(new BigDecimal(count));
            totalAmount = totalAmount.add(itemTotal);

            // 准备明细
            OrderItem item = new OrderItem();
            item.setProductId(productId);
            item.setPrice(product.getPrice());
            item.setCount(count);
            items.add(item);
        }

        // 2. 创建订单
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(0); // 待支付
        order.setGmtCreate(LocalDateTime.now());
        order.setGmtModified(LocalDateTime.now());
        this.save(order);

        // 3. 保存明细
        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        log.info("Order created: {}, User: {}, Amount: {}", order.getId(), userId, totalAmount);
        return order.getId();
    }

    /**
     * 取消订单
     * @param userId 用户ID
     * @param orderId 订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, Long orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (order.getStatus() != 0) { // 仅待支付状态可取消
            throw new BusinessException(400, "当前订单状态不可取消");
        }

        // 1. 更新状态
        order.setStatus(4); // 已取消
        order.setGmtModified(LocalDateTime.now());
        this.updateById(order);

        // 2. 恢复库存
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        if (items != null) {
            for (OrderItem it : items) {
                if (it == null || it.getProductId() == null || it.getCount() == null) continue;
                productService.recoverStock(it.getProductId(), it.getCount());
            }
        }
    }

    /**
     * 支付成功回调
     * @param orderId 订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            return; // 幂等处理，忽略不存在
        }
        if (order.getStatus() != 0) {
            log.warn("Order {} status is {}, ignore payment callback", orderId, order.getStatus());
            return;
        }

        order.setStatus(1); // 已支付
        order.setPayTime(LocalDateTime.now());
        order.setGmtModified(LocalDateTime.now());
        this.updateById(order);

        // 更新用户消费总额
        userService.updateTotalSpend(order.getUserId(), order.getTotalAmount());
        
        log.info("Order paid: {}", orderId);
    }

    @Override
    public Page<OrderVO> pageMyOrders(Long userId, int page, int size, Integer status) {
        Page<Order> p = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getGmtCreate);
        this.page(p, wrapper);

        List<Order> orders = p.getRecords();
        if (orders == null || orders.isEmpty()) {
            Page<OrderVO> result = new Page<>(page, size);
            result.setTotal(p.getTotal());
            result.setSize(p.getSize());
            result.setCurrent(p.getCurrent());
            result.setPages(p.getPages());
            result.setRecords(List.of());
            return result;
        }

        Set<Long> orderIds = orders.stream().map(Order::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, List<OrderItem>> itemMap = loadOrderItems(orderIds);
        Map<Long, Product> productMap = loadProducts(itemMap.values().stream().flatMap(List::stream).map(OrderItem::getProductId).collect(Collectors.toSet()));

        List<OrderVO> voList = orders.stream().map(o -> buildOrderVO(o, itemMap.getOrDefault(o.getId(), List.of()), productMap)).collect(Collectors.toList());
        Page<OrderVO> result = new Page<>(page, size);
        result.setTotal(p.getTotal());
        result.setSize(p.getSize());
        result.setCurrent(p.getCurrent());
        result.setPages(p.getPages());
        result.setRecords(voList);
        return result;
    }

    @Override
    public OrderVO getMyOrder(Long userId, Long orderId) {
        Order o = this.getById(orderId);
        if (o == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (!Objects.equals(o.getUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        Map<Long, Product> productMap = loadProducts(items == null ? Set.of() : items.stream().map(OrderItem::getProductId).collect(Collectors.toSet()));
        return buildOrderVO(o, items == null ? List.of() : items, productMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliverOrder(Long userId, Long orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new BusinessException(400, "当前订单状态不可发货");
        }
        order.setStatus(2);
        order.setGmtModified(LocalDateTime.now());
        this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishOrder(Long userId, Long orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (order.getStatus() == null || (order.getStatus() != 1 && order.getStatus() != 2)) {
            throw new BusinessException(400, "当前订单状态不可完成");
        }
        order.setStatus(3);
        order.setGmtModified(LocalDateTime.now());
        this.updateById(order);
    }

    private Map<Long, List<OrderItem>> loadOrderItems(Set<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));
        if (items == null || items.isEmpty()) {
            return Map.of();
        }
        return items.stream().filter(Objects::nonNull).collect(Collectors.groupingBy(OrderItem::getOrderId));
    }

    private Map<Long, Product> loadProducts(Set<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        List<Product> products = productService.listByIds(productIds);
        if (products == null || products.isEmpty()) {
            return Map.of();
        }
        return products.stream().filter(Objects::nonNull).collect(Collectors.toMap(Product::getId, Function.identity(), (a, b) -> a));
    }

    private OrderVO buildOrderVO(Order o, List<OrderItem> items, Map<Long, Product> productMap) {
        OrderVO vo = new OrderVO();
        vo.setId(o.getId());
        vo.setTotalAmount(o.getTotalAmount());
        vo.setStatus(o.getStatus());
        vo.setPayTime(o.getPayTime());
        vo.setGmtCreate(o.getGmtCreate());

        if (items == null || items.isEmpty()) {
            vo.setItems(List.of());
            return vo;
        }
        List<OrderItemVO> itemVos = new ArrayList<>();
        for (OrderItem it : items) {
            if (it == null) continue;
            OrderItemVO iv = new OrderItemVO();
            iv.setProductId(it.getProductId());
            iv.setCount(it.getCount());
            iv.setPrice(it.getPrice());
            Product p = productMap == null ? null : productMap.get(it.getProductId());
            if (p != null) {
                iv.setProductTitle(p.getTitle());
            }
            itemVos.add(iv);
        }
        vo.setItems(itemVos);
        return vo;
    }
}
