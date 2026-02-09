package com.li.lwg.dto;

/**
 * @author liyuelian
 * @date 2026/2/9
 * 任务提交dto
 */
public class MissionSubmitReq {
    /**
     * 任务ID
     */
    private Long missionId;

    /**
     * 提交者ID (当前登录用户)
     */
    private Long userId;

    private Object proofData;

    public Object getProofData() {
        return proofData;
    }

    public void setProofData(Object proofData) {
        this.proofData = proofData;
    }

    public Long getMissionId() {
        return missionId;
    }

    public void setMissionId(Long missionId) {
        this.missionId = missionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
