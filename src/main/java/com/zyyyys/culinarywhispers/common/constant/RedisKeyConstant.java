package com.zyyyys.culinarywhispers.common.constant;

/**
 * Redis Key 常量
 * @author zyyyys
 */
public class RedisKeyConstant {
    /**
     * 食谱统计 Hash Key 前缀
     * 格式: cw:stats:recipe:{recipeId}
     * 字段: view_count, like_count, ...
     */
    public static final String RECIPE_STATS_PREFIX = "cw:stats:recipe:";

    /**
     * 待同步的食谱ID集合 (Set)
     * 存储发生过变动的食谱ID
     */
    public static final String RECIPE_STATS_DIRTY_SET = "cw:stats:dirty_recipes";
}
