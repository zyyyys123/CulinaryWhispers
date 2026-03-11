package com.zyyyys.culinarywhispers.module.commerce.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class OrderItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long productId;
    private String productTitle;
    private Integer count;
    private BigDecimal price;
}

