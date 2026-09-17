package com.li.lwg.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @author liyuelian
 * @date 2026/2/23
 * 取消任务请求对象
 */
public class MissionCancelReq {
    /**
     * 任务ID
     */
    @NotNull(message = "任务ID不能为空")
    private Long missionId;

    /**
     * 发起撤销的当前用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 取消原因
     * 上限 255 与 t_mission.cancel_reason 的 varchar(255) 对齐，
     * 避免超长内容穿透到数据库抛 DataTooLong，最终以 500 暴露给调用方
     */
    @Size(max = 255, message = "取消原因最多 255 个字符")
    private String cancelReason;

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

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}