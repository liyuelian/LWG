package com.li.lwg.entity;

import java.time.LocalDateTime;

/**
 * 信誉变更流水表
 */
public class ReputationLog {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 信誉变动值
     */
    private Integer changeScore;

    /**
     * 变动后的当前信誉值
     */
    private Integer currentScore;

    /**
     * 来源类型: 1-任务结算, 2-违约扣除, 3-任务截止时间超期
     */
    private Integer sourceType;

    /**
     * 关联ID (如任务ID)
     */
    private Long sourceId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getChangeScore() {
        return changeScore;
    }

    public void setChangeScore(Integer changeScore) {
        this.changeScore = changeScore;
    }

    public Integer getCurrentScore() {
        return currentScore;
    }

    public void setCurrentScore(Integer currentScore) {
        this.currentScore = currentScore;
    }

    public Integer getSourceType() {
        return sourceType;
    }

    public void setSourceType(Integer sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}