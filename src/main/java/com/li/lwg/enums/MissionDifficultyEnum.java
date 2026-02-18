package com.li.lwg.enums;

import java.util.Arrays;

/**
 * 任务难度类型枚举
 */
public enum MissionDifficultyEnum {

    SIMPLE(1, "简单", 25),
    NORMAL(2, "普通", 50),
    HARD(3, "困难", 100),
    HELL(4, "地狱", 200);

    private final int code;
    private final String desc;
    private final int score;

    public static MissionDifficultyEnum getByCode(Integer code) {
        if (code == null) return SIMPLE;
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElse(SIMPLE);
    }

    MissionDifficultyEnum(int code, String desc, int score) {
        this.code = code;
        this.desc = desc;
        this.score = score;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public int getScore() {
        return score;
    }
}