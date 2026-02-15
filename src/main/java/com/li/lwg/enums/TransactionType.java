package com.li.lwg.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liyuelian
 * @date 2026/2/10
 * 资金动账类型枚举
 */
public enum TransactionType {

    /**
     * 1: 发布悬赏 (涉及：可用余额减少 + 冻结余额增加)
     */
    PUBLISH(1, "发布悬赏", 0),

    /**
     * 2: 任务结算-支出 (涉及：冻结余额减少)
     */
    SETTLEMENT(2, "结算支出", -1),

    /**
     * 3: 任务结算-收入 (涉及：可用余额增加)
     */
    INCOME(3, "任务收益", 1),

    /**
     * 4: 任务取消/驳回退款 (涉及：冻结余额减少 + 可用余额增加) - 预留
     */
    REFUND(4, "悬赏退回", 0),
    /**
     * 5：灵石充值-收入
     */
    RECHARGE(5, "灵石充值", 1);


    private final int code;
    private final String desc;
    private final int category; // 1:真实收入, -1:真实支出, 0:中性

    /**
     * 获取所有【真实收入】的类型ID列表
     */
    public static List<Integer> getRealIncomeTypes() {
        return Arrays.stream(values())
                .filter(t -> t.category == 1)
                .map(TransactionType::getCode)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有【真实支出】的类型ID列表
     */
    public static List<Integer> getRealExpenseTypes() {
        return Arrays.stream(values())
                .filter(t -> t.category == -1)
                .map(TransactionType::getCode)
                .collect(Collectors.toList());
    }

    TransactionType(int code, String desc, int category) {
        this.code = code;
        this.desc = desc;
        this.category = category;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public int getCategory() {
        return category;
    }
}