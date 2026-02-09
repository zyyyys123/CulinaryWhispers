package com.zyyyys.culinarywhispers.common.utils;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NutritionCalculatorTest {

    @Test
    public void testCalculatePercentage() {
        assertEquals(50, NutritionCalculator.calculatePercentage(1000, 2000));
        assertEquals(0, NutritionCalculator.calculatePercentage(null, 2000));
        assertEquals(0, NutritionCalculator.calculatePercentage(1000, 0));
    }

    @Test
    public void testCalculateCaloriesPercentage() {
        assertEquals(25, NutritionCalculator.calculateCaloriesPercentage(500));
    }

    @Test
    public void testCalculateProteinPercentage() {
        // 60g is 100%
        assertEquals(50, NutritionCalculator.calculateProteinPercentage(new BigDecimal("30.0")));
    }

    @Test
    public void testCalculateFatPercentage() {
        // 60g is 100%
        assertEquals(100, NutritionCalculator.calculateFatPercentage(new BigDecimal("60.0")));
    }

    @Test
    public void testCalculateCarbsPercentage() {
        // 300g is 100%
        assertEquals(33, NutritionCalculator.calculateCarbsPercentage(new BigDecimal("100.0")));
    }
}
