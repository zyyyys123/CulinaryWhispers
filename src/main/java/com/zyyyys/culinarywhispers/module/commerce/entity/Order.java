package com.zyyyys.culinarywhispers.module.commerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类
 * @author zyyyys
 */
@Data
@TableName("t_comm_order")
@Schema(name = "Order", description = "市集订单")
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "订单ID", example = "10001")
    private Long id;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    /**
     * 订单总金额
     */
    @Schema(description = "订单总金额", example = "398.00")
    private BigDecimal totalAmount;

    /**
     * 订单状态: 0-待支付, 1-已支付, 2-已发货, 3-已完成, 4-已取消
     */
    @Schema(description = "订单状态：0-待支付,1-已支付,2-已发货,3-已完成,4-已取消", example = "0")
    private Integer status;

    /**
     * 支付时间
     */
    @Schema(description = "支付时间", example = "2026-03-07T12:05:00")
    private LocalDateTime payTime;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-03-07T12:00:00")
    private LocalDateTime gmtCreate;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2026-03-07T12:00:00")
    private LocalDateTime gmtModified;
}
