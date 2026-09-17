package com.li.lwg.enums;

/**
 * 宗门悬赏（任务）状态枚举
 */
public enum MissionStatusEnum {

    /**
     * 0: 待接 (悬赏已发布，等待道友揭榜)
     */
    PENDING_ACCEPT(0, "待接单"),

    /**
     * 1: 进行 (已被接取，修仙者正在执行)
     */
    IN_PROGRESS(1, "进行中"),

    /**
     * 2: 待验 (执行者已提交复命，等待发布者验收)
     */
    PENDING_VERIFY(2, "待验收"),

    /**
     * 3: 完成 (发布者已验收通过，赏金已结算)
     */
    COMPLETED(3, "已完成"),

    /**
     * 4: 已取消 (发布者主动撤榜)
     */
    CANCELLED(4, "已取消");

    private final Integer code;
    private final String desc;


    /**
     * 根据状态码获取对应的描述
     *
     * @param code 状态码
     * @return 状态描述
     */
    public static String getDescByCode(Integer code) {
        if (code == null) {
            return "未知状态";
        }
        for (MissionStatusEnum status : MissionStatusEnum.values()) {
            if (status.getCode().equals(code)) {
                return status.getDesc();
            }
        }
        return "未知状态";
    }

    /**
     * 根据状态码获取枚举对象
     *
     * @param code 状态码
     * @return 枚举对象
     */
    public static MissionStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (MissionStatusEnum status : MissionStatusEnum.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    MissionStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}