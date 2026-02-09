package com.li.lwg.dto;

/**
 * @author liyuelian
 * @date 2026/2/9
 * 任务查询dto
 */
public class MissionQueryReq {
    /**
     * 任务类型
     */
    private Integer missionType;

    /**
     * 任务难度
     */
    private Integer difficulty;

    /**
     * 最低境界
     */
    private Integer minRealm;

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 任务状态
     * 0-待接单, 1-进行中, 2-待结算, 3-已完成, 4-已取消
     */
    private Integer status;

    // 后面如果做分页，这里可以加 page, size


    public Integer getMissionType() {
        return missionType;
    }

    public void setMissionType(Integer missionType) {
        this.missionType = missionType;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getMinRealm() {
        return minRealm;
    }

    public void setMinRealm(Integer minRealm) {
        this.minRealm = minRealm;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
