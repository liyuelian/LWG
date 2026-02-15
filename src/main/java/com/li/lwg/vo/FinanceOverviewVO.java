package com.li.lwg.vo;

import java.io.Serializable;

/**
 * 财务概览视图对象
 */
public class FinanceOverviewVO implements Serializable {
    // 累计总收入
    private Long totalIncome;

    // 累计总支出
    private Long totalExpense;

    // 本月收入
    private Long monthIncome;

    // 本月支出
    private Long monthExpense;

    public Long getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(Long totalIncome) {
        this.totalIncome = totalIncome;
    }

    public Long getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(Long totalExpense) {
        this.totalExpense = totalExpense;
    }

    public Long getMonthIncome() {
        return monthIncome;
    }

    public void setMonthIncome(Long monthIncome) {
        this.monthIncome = monthIncome;
    }

    public Long getMonthExpense() {
        return monthExpense;
    }

    public void setMonthExpense(Long monthExpense) {
        this.monthExpense = monthExpense;
    }
}