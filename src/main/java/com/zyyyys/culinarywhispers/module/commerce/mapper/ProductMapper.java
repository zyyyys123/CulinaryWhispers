package com.zyyyys.culinarywhispers.module.commerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyyyys.culinarywhispers.module.commerce.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 商品Mapper接口
 * @author zyyyys
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 扣减库存 (乐观锁)
     * @param id 商品ID
     * @param count 扣减数量
     * @return 影响行数
     */
    @Update("UPDATE t_comm_product SET stock = stock - #{count} WHERE id = #{id} AND stock >= #{count}")
    int reduceStock(Long id, Integer count);
    
    /**
     * 恢复库存
     * @param id 商品ID
     * @param count 恢复数量
     */
    @Update("UPDATE t_comm_product SET stock = stock + #{count} WHERE id = #{id}")
    int recoverStock(Long id, Integer count);
}
