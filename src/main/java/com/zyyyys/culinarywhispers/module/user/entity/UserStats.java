package com.zyyyys.culinarywhispers.module.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户统计表实体类
 * @author zyyyys
 */
@Data
@TableName("t_usr_stats")
public class UserStats implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId
    private Long userId;

    /**
     * 用户等级
     */
    private Integer level;

    /**
     * 经验值
     */
    private Long experience;

    /**
     * 发布食谱数
     */
    private Integer totalRecipes;

    /**
     * 发布动态数
     */
    private Integer totalMoments;

    /**
     * 获赞总数
     */
    private Long totalLikesReceived;

    /**
     * 被收藏总数
     */
    private Long totalCollectsReceived;

    /**
     * 粉丝数
     */
    private Integer totalFans;

    /**
     * 关注数
     */
    private Integer totalFollows;

    /**
     * 主页访问量
     */
    private Long totalViews;

    /**
     * 周活跃天数
     */
    private Integer weekActiveDays;

    /**
     * 月活跃天数
     */
    private Integer monthActiveDays;

    /**
     * 最后发布时间
     */
    private LocalDateTime lastPublishTime;

    /**
     * 积分余额 (5.2 积分生态)
     */
    private Integer points;

    /**
     * 连续签到天数 (5.2 积分生态)
     */
    private Integer continueSignDays;

    /**
     * 上次签到日期 (5.2 积分生态)
     */
    private java.time.LocalDate lastSignDate;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;
}
