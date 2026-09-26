package com.li.lwg.enums;

import com.li.lwg.exception.ServiceException;

/** 天道碑支持的榜单类型。 */
public enum RankTypeEnum {
    REPUTATION("reputation"),
    COMPLETED("completed");

    private final String code;

    RankTypeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static RankTypeEnum fromCode(String code) {
        for (RankTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new ServiceException("榜单类型仅支持 reputation（信誉）或 completed（悬赏完成）");
    }
}
