package com.zyyyys.culinarywhispers.module.commerce.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.module.commerce.entity.Product;
import com.zyyyys.culinarywhispers.module.commerce.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productService, "baseMapper", productMapper);
    }

    @Test
    void reduceStock_Success() {
        Long productId = 1L;
        Integer count = 2;
        when(productMapper.reduceStock(productId, count)).thenReturn(1);

        boolean result = productService.reduceStock(productId, count);

        assertTrue(result);
        verify(productMapper).reduceStock(productId, count);
    }

    @Test
    void reduceStock_Fail_NotEnough() {
        Long productId = 1L;
        Integer count = 100;
        when(productMapper.reduceStock(productId, count)).thenReturn(0);

        boolean result = productService.reduceStock(productId, count);

        assertFalse(result);
    }
}
