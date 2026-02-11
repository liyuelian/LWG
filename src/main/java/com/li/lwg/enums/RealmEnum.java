package com.li.lwg.enums;

import lombok.Getter;

/**
 * @author liyuelian
 * @date 2026/2/8
 */
@Getter
public enum RealmEnum {
    /**
     * 境界:
     */
    LIAN_QI(1, "炼气期"),
    ZHU_JI(2, "筑基期"),
    JIN_DAN(3, "金丹期"),
    YUAN_YING(4, "元婴期"),
    HUA_SHEN(5, "化神期"),
    LIAN_XU(6, "炼虚期"),
    HE_TI(7, "合体期"),
    DA_CHENG(8, "大乘期"),
    DU_JIE(9, "渡劫期");

    private final int code;
    private final String desc;

    RealmEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 增加一个静态方法：根据数字找枚举（方便回显）
    public static RealmEnum fromCode(int code) {
        for (RealmEnum value : RealmEnum.values()) {
            if (value.code == code) {
                return value;
            }
        }
        return LIAN_QI; // 默认值
    }
}