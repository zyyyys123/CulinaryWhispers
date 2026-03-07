package com.zyyyys.culinarywhispers.module.commerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(name = "CreateOrderRequest", description = "创建订单请求")
public class CreateOrderRequest {
    @Schema(
            description = "商品ID与购买数量映射（key=productId, value=count）",
            example = "{\"1\":2,\"3\":1}",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Map<Long, Integer> productCounts;
}

