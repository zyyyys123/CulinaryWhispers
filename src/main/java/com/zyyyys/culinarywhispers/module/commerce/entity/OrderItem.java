package com.zyyyys.culinarywhispers.module.commerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细实体类
 * @author zyyyys
 */
@Data
@TableName("t_comm_order_item")
public class OrderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 购买单价
     */
    private BigDecimal price;

    /**
     * 购买数量
     */
    private Integer count;
}
