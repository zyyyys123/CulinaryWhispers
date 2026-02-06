package com.zyyyys.culinarywhispers.module.commerce.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zyyyys.culinarywhispers.module.commerce.entity.Product;

/**
 * 商品服务接口
 * @author zyyyys
 */
public interface ProductService extends IService<Product> {

    /**
     * 商品分页查询
     * @param keyword 关键词
     * @param categoryId 分类ID
     * @param page 页码
     * @param size 每页大小
     * @return 分页列表
     */
    Page<Product> listProducts(String keyword, Integer categoryId, int page, int size);

    /**
     * 扣减库存
     * @param productId 商品ID
     * @param count 数量
     * @return 是否成功
     */
    boolean reduceStock(Long productId, Integer count);

    /**
     * 恢复库存
     * @param productId 商品ID
     * @param count 数量
     */
    void recoverStock(Long productId, Integer count);
}
