package com.zyyyys.culinarywhispers.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 营养计算工具类
 * 基于中国居民膳食指南 (2022) 参考值
 * @author zyyyys
 */
public class NutritionCalculator {

    // 成人每日推荐摄入量 (RNI) 参考平均值 (轻体力劳动)
    public static final int DAILY_CALORIES_RNI = 2000; // kcal
    public static final double DAILY_PROTEIN_RNI = 60.0; // g
    public static final double DAILY_FAT_RNI = 60.0; // g (占总热量 20-30%, 约 40-60g, 取上限)
    public static final double DAILY_CARBS_RNI = 300.0; // g (占总热量 50-65%, 约 250-325g)

    /**
     * 计算单项营养素占每日推荐摄入量的百分比
     * @param value 摄入量
     * @param rni 推荐摄入量
     * @return 百分比 (0-100)
     */
    public static int calculatePercentage(Number value, Number rni) {
        if (value == null || rni == null || rni.doubleValue() <= 0) {
            return 0;
        }
        double percentage = (value.doubleValue() / rni.doubleValue()) * 100;
        return (int) Math.round(percentage);
    }
    
    /**
     * 计算卡路里占比
     */
    public static int calculateCaloriesPercentage(Integer calories) {
        return calculatePercentage(calories, DAILY_CALORIES_RNI);
    }

    /**
     * 计算蛋白质占比
     */
    public static int calculateProteinPercentage(BigDecimal protein) {
        return calculatePercentage(protein, DAILY_PROTEIN_RNI);
    }

    /**
     * 计算脂肪占比
     */
    public static int calculateFatPercentage(BigDecimal fat) {
        return calculatePercentage(fat, DAILY_FAT_RNI);
    }

    /**
     * 计算碳水占比
     */
    public static int calculateCarbsPercentage(BigDecimal carbs) {
        return calculatePercentage(carbs, DAILY_CARBS_RNI);
    }
}
