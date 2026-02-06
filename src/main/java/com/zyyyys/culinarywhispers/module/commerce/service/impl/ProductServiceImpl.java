package com.zyyyys.culinarywhispers.module.commerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyyyys.culinarywhispers.module.commerce.entity.Product;
import com.zyyyys.culinarywhispers.module.commerce.mapper.ProductMapper;
import com.zyyyys.culinarywhispers.module.commerce.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 商品服务实现类
 * @author zyyyys
 */
@Slf4j
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    /**
     * 列表查询商品
     * @param keyword 关键词
     * @param categoryId 分类ID
     * @param page 页码
     * @param size 每页大小
     * @return 商品分页列表
     */
    @Override
    public Page<Product> listProducts(String keyword, Integer categoryId, int page, int size) {
        Page<Product> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        
        wrapper.like(StringUtils.hasText(keyword), Product::getTitle, keyword)
               .eq(categoryId != null, Product::getCategoryId, categoryId)
               .orderByDesc(Product::getGmtCreate);
               
        return this.page(pageParam, wrapper);
    }

    /**
     * 减少商品库存
     * @param productId 商品ID
     * @param count 减少数量
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reduceStock(Long productId, Integer count) {
        if (count <= 0) {
            return false;
        }
        int rows = baseMapper.reduceStock(productId, count);
        return rows > 0;
    }

    /**
     * 恢复商品库存
     * @param productId 商品ID
     * @param count 恢复数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverStock(Long productId, Integer count) {
        if (count <= 0) {
            return;
        }
        baseMapper.recoverStock(productId, count);
    }
}
