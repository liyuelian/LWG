package com.li.lwg.enums;

/**
 * 资产类型枚举
 * 用于标记动的是“哪个钱包”的钱
 */
public enum AssetType {

    /**
     * 1: 可用余额 (Available Balance) - 可以随意消费、提现
     */
    AVAILABLE(1, "可用余额"),

    /**
     * 2: 冻结余额 (Frozen Balance) - 已锁定，不可挪作他用
     */
    FROZEN(2, "冻结余额");

    private final int code;
    private final String desc;

    AssetType(int code, String desc) {
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