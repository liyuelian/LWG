package com.li.lwg.dto;

import java.io.Serializable;

/**
 * 任务消息传输mq对象
 */
public class MissionSettledMsg implements Serializable {
    private Long userId;    // 接单人
    private boolean pass;   // 是否通过
    private Integer difficulty;  // 难度
    private Long missionId; // 任务ID

    public MissionSettledMsg(Long userId, boolean pass, Integer difficulty, Long missionId) {
        this.userId = userId;
        this.pass = pass;
        this.difficulty = difficulty;
        this.missionId = missionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isPass() {
        return pass;
    }

    public void setPass(boolean pass) {
        this.pass = pass;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public Long getMissionId() {
        return missionId;
    }

    public void setMissionId(Long missionId) {
        this.missionId = missionId;
    }
}