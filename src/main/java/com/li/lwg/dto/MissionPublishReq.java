package com.li.lwg.dto;

/**
 * @author liyuelian
 * @date 2026/2/8
 * 任务发布dto
 */
public class MissionPublishReq {
    private Long publisherId; // 实际项目中这个应该从 Token 获取，MVP先传参
    private String title;
    private String description;
    private Integer missionType;
    private Integer difficulty;
    private Integer minRealm;
    private Long reward;

    public Long getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(Long publisherId) {
        this.publisherId = publisherId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

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

    public Long getReward() {
        return reward;
    }

    public void setReward(Long reward) {
        this.reward = reward;
    }
}