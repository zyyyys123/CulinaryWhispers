package com.zyyyys.culinarywhispers.module.commerce.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(name = "OrderVO", description = "订单展示对象（含明细）")
public class OrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID", example = "10001")
    private Long id;
    @Schema(description = "订单总金额", example = "398.00")
    private BigDecimal totalAmount;
    @Schema(description = "订单状态：0-待支付,1-已支付,2-已发货,3-已完成,4-已取消", example = "1")
    private Integer status;
    @Schema(description = "支付时间", example = "2026-03-07T12:05:00")
    private LocalDateTime payTime;
    @Schema(description = "创建时间", example = "2026-03-07T12:00:00")
    private LocalDateTime gmtCreate;
    @Schema(description = "订单明细")
    private List<OrderItemVO> items;
}

