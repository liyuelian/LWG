package com.li.lwg.dto;

/**
 * @author liyuelian
 * @date 2026/2/9
 */
public class MissionAcceptReq {
    /**
     * 要抢的任务ID
     */
    private Long missionId;

    /**
     * 抢单者ID (实际项目从Token取，这里先传参)
     */
    private Long acceptorId;

    public Long getMissionId() {
        return missionId;
    }

    public void setMissionId(Long missionId) {
        this.missionId = missionId;
    }

    public Long getAcceptorId() {
        return acceptorId;
    }

    public void setAcceptorId(Long acceptorId) {
        this.acceptorId = acceptorId;
    }
}