package com.zyyyys.culinarywhispers.module.commerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyyyys.culinarywhispers.module.commerce.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细Mapper接口
 * @author zyyyys
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
