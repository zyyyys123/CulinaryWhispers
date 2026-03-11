package com.zyyyys.culinarywhispers.module.commerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细实体类
 * @author zyyyys
 */
@Data
@TableName("t_comm_order_item")
@Schema(name = "OrderItem", description = "订单明细")
public class OrderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "明细ID", example = "50001")
    private Long id;

    /**
     * 订单ID
     */
    @Schema(description = "订单ID", example = "10001")
    private Long orderId;

    /**
     * 商品ID
     */
    @Schema(description = "商品ID", example = "1")
    private Long productId;

    /**
     * 购买单价
     */
    @Schema(description = "购买单价", example = "199.00")
    private BigDecimal price;

    /**
     * 购买数量
     */
    @Schema(description = "购买数量", example = "2")
    private Integer count;
}
