package com.li.lwg.common;

/**
 * @author liyuelian
 * @date 2026/2/10
 * 资金动账类型枚举
 */

public enum TransactionType {

    /**
     * 1: 发布任务 (余额 -> 冻结)
     */
    PUBLISH(1, "发布悬赏"),

    /**
     * 2: 任务结算-支出 (冻结 -> 扣除)
     */
    SETTLEMENT(2, "结算支出"),

    /**
     * 3: 任务结算-收入 (系统 -> 余额)
     */
    INCOME(3, "任务收益"),

    /**
     * 4: 充值 (预留)
     */
    RECHARGE(4, "充值"),

    /**
     * 5: 提现 (预留)
     */
    WITHDRAW(5, "提现");

    private final int code;
    private final String desc;

    TransactionType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}