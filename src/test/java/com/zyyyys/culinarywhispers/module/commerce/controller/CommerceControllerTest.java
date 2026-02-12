package com.zyyyys.culinarywhispers.module.commerce.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.common.context.UserContext;
import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.module.commerce.entity.Product;
import com.zyyyys.culinarywhispers.module.commerce.service.OrderService;
import com.zyyyys.culinarywhispers.module.commerce.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommerceControllerTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void product_and_order_endpoints() {
        ProductService productService = mock(ProductService.class);
        OrderService orderService = mock(OrderService.class);
        CommerceController controller = new CommerceController(productService, orderService);

        when(productService.listProducts(any(), any(), anyInt(), anyInt())).thenReturn(new Page<Product>());
        when(productService.getById(1L)).thenReturn(new Product());

        UserContext.setUserId(1L);
        when(orderService.createOrder(eq(1L), any())).thenReturn(10L);

        Result<Page<Product>> list = controller.listProducts(null, null, 1, 10);
        assertEquals(0, list.getCode());

        Result<Product> detail = controller.getProduct(1L);
        assertEquals(0, detail.getCode());

        Result<Long> orderId = controller.createOrder(Map.of(1L, 1));
        assertEquals(0, orderId.getCode());
    }
}
