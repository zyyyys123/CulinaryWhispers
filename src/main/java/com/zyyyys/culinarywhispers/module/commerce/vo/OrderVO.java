package com.zyyyys.culinarywhispers.module.commerce.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private BigDecimal totalAmount;
    private Integer status;
    private LocalDateTime payTime;
    private LocalDateTime gmtCreate;
    private List<OrderItemVO> items;
}

