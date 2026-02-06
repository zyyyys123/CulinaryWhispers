package com.zyyyys.culinarywhispers.module.commerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyyyys.culinarywhispers.module.commerce.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单Mapper接口
 * @author zyyyys
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
