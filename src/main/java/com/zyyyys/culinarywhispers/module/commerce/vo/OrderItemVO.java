package com.zyyyys.culinarywhispers.module.commerce.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(name = "OrderItemVO", description = "订单明细展示对象")
public class OrderItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID", example = "1")
    private Long productId;
    @Schema(description = "商品标题", example = "不粘锅 28cm")
    private String productTitle;
    @Schema(description = "购买数量", example = "2")
    private Integer count;
    @Schema(description = "购买单价", example = "199.00")
    private BigDecimal price;
}

