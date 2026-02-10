package com.li.lwg.dto;

/**
 * @author liyuelian
 * @date 2026/2/10
 */
public class MissionAuditReq {
    /**
     * 任务ID
     */
    private Long missionId;

    /**
     * 审核人ID (必须是该任务的发布者)
     */
    private Long userId;

    /**
     * 审核结果
     * true: 通过 (发布者满意，系统打钱)
     * false: 驳回 (发布者不满意，任务重置为“进行中”，让弟子重做)
     */
    private Boolean pass;

    /**
     * 审核备注 / 驳回理由
     * 例如："做得不错" 或 "妖丹成色不足，请重新获取"
     */
    private String remark;

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

    public Boolean getPass() {
        return pass;
    }

    public void setPass(Boolean pass) {
        this.pass = pass;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}