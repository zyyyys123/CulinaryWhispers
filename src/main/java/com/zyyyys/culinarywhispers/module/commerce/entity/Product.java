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
 * 商品实体类
 * @author zyyyys
 */
@Data
@TableName("t_comm_product")
@Schema(name = "Product", description = "市集商品")
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "商品ID", example = "1")
    private Long id;

    /**
     * 商品标题
     */
    @Schema(description = "商品标题", example = "不粘锅 28cm")
    private String title;

    /**
     * 商品描述
     */
    @Schema(description = "商品描述", example = "适合新手的家用不粘锅，易清洗。")
    private String description;

    /**
     * 单价
     */
    @Schema(description = "单价", example = "199.00")
    private BigDecimal price;

    /**
     * 库存
     */
    @Schema(description = "库存", example = "100")
    private Integer stock;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID", example = "10")
    private Integer categoryId;

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
