package com.li.lwg.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 财务图表视图对象
 */
public class FinanceChartVO implements Serializable {

    // 折线图数据 (X轴: 月份, Y轴: 金额)
    private List<String> trendMonths;
    private List<Long> trendIncome;
    private List<Long> trendExpense;

    // 饼图数据
    private List<PieItem> pieData;

    public static class PieItem {
        private String name;
        private Long value;

        public PieItem(String name, Long value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getValue() {
            return value;
        }

        public void setValue(Long value) {
            this.value = value;
        }
    }

    public FinanceChartVO() {
    }


    public List<String> getTrendMonths() {
        return trendMonths;
    }

    public void setTrendMonths(List<String> trendMonths) {
        this.trendMonths = trendMonths;
    }

    public List<Long> getTrendIncome() {
        return trendIncome;
    }

    public void setTrendIncome(List<Long> trendIncome) {
        this.trendIncome = trendIncome;
    }

    public List<Long> getTrendExpense() {
        return trendExpense;
    }

    public void setTrendExpense(List<Long> trendExpense) {
        this.trendExpense = trendExpense;
    }

    public List<PieItem> getPieData() {
        return pieData;
    }

    public void setPieData(List<PieItem> pieData) {
        this.pieData = pieData;
    }
}