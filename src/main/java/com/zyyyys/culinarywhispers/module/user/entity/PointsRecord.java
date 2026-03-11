package com.zyyyys.culinarywhispers.module.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分流水表实体类
 * @author zyyyys
 */
@Data
@TableName("t_points_record")
@Schema(name = "PointsRecord", description = "积分流水记录")
public class PointsRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "记录ID", example = "100")
    private Long id;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    /**
     * 变动类型: 1-签到, 2-发布食谱, 3-被点赞, 10-兑换商品
     */
    @Schema(description = "变动类型：1-签到,2-发布食谱,3-被点赞,10-兑换商品", example = "1")
    private Integer type;

    /**
     * 变动数量 (正数增加，负数扣除)
     */
    @Schema(description = "变动数量（正数增加，负数扣除）", example = "5")
    private Integer amount;

    /**
     * 变动说明
     */
    @Schema(description = "变动说明", example = "每日签到 +5")
    private String description;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间", example = "2026-03-11T10:00:00")
    private LocalDateTime gmtCreate;
}
