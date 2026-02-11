package com.li.lwg.enums;

/**
 * @author liyuelian
 * @date 2026/2/10
 * 资金动账类型枚举
 */

public enum TransactionType {

    /**
     * 1: 发布悬赏 (涉及：可用余额减少 + 冻结余额增加)
     */
    PUBLISH(1, "发布悬赏"),

    /**
     * 2: 任务结算-支出 (涉及：冻结余额减少)
     */
    SETTLEMENT(2, "结算支出"),

    /**
     * 3: 任务结算-收入 (涉及：可用余额增加)
     */
    INCOME(3, "任务收益"),

    /**
     * 4: 任务取消/驳回退款 (涉及：冻结余额减少 + 可用余额增加) - 预留
     */
    REFUND(4, "悬赏退回");


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