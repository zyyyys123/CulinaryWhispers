package com.zyyyys.culinarywhispers.module.commerce.service.impl;

import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.module.commerce.entity.Order;
import com.zyyyys.culinarywhispers.module.commerce.entity.OrderItem;
import com.zyyyys.culinarywhispers.module.commerce.entity.Product;
import com.zyyyys.culinarywhispers.module.commerce.mapper.OrderItemMapper;
import com.zyyyys.culinarywhispers.module.commerce.mapper.OrderMapper;
import com.zyyyys.culinarywhispers.module.commerce.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "baseMapper", orderMapper);
    }

    @Test
    void createOrder_Success() {
        Long userId = 1L;
        Long productId = 100L;
        Integer count = 2;
        Map<Long, Integer> items = new HashMap<>();
        items.put(productId, count);

        Product product = new Product();
        product.setId(productId);
        product.setPrice(new BigDecimal("50.00"));
        product.setTitle("Steak");

        when(productService.getById(productId)).thenReturn(product);
        when(productService.reduceStock(productId, count)).thenReturn(true);
        when(orderMapper.insert(any(Order.class))).thenReturn(1);
        when(orderItemMapper.insert(any(OrderItem.class))).thenReturn(1);

        Long orderId = orderService.createOrder(userId, items);

        verify(productService).reduceStock(productId, count);
        verify(orderMapper).insert(any(Order.class));
        verify(orderItemMapper).insert(any(OrderItem.class));
    }

    @Test
    void createOrder_Fail_StockNotEnough() {
        Long userId = 1L;
        Long productId = 100L;
        Map<Long, Integer> items = Collections.singletonMap(productId, 10);

        Product product = new Product();
        product.setId(productId);
        product.setPrice(BigDecimal.TEN);

        when(productService.getById(productId)).thenReturn(product);
        when(productService.reduceStock(productId, 10)).thenReturn(false);

        assertThrows(BusinessException.class, () -> 
            orderService.createOrder(userId, items));
    }

    @Test
    void paySuccess_Success() {
        Long orderId = 1L;
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(0); // Created

        when(orderMapper.selectById(orderId)).thenReturn(order);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        orderService.paySuccess(orderId);

        assertEquals(1, order.getStatus()); // Paid
        assertNotNull(order.getPayTime());
    }
}
