package com.li.lwg.enums;

/**
 * 信誉流水来源类型枚举
 */
public enum ReputationSourceEnum {

    /**
     * 任务结算 (加分/扣分)
     * source_id = mission_id
     */
    MISSION_SETTLED(1, "任务结算"),

    /**
     * 任务超时 (扣分)
     * source_id = mission_id
     */
    MISSION_TIMEOUT(2, "任务超时");

    private final int code;
    private final String desc;

    /**
     * 根据 code 获取枚举
     */
    public static ReputationSourceEnum getByCode(Integer code) {
        if (code == null) return null;
        for (ReputationSourceEnum e : values()) {
            if (e.code == code) return e;
        }
        return null;
    }

    ReputationSourceEnum(int code, String desc) {
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