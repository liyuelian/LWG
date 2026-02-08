package com.li.lwg.entity;

import java.time.LocalDateTime;

/**
 * @author liyuelian
 * @date 2026/2/8
 * @desc 任务实体
 */
public class Mission {
    private Long id;

    /**
     * 任务标题
     */
    private String title;

    /**
     * 任务详情
     */
    private String description;

    /**
     * 类型: 1-降妖,2-采集,3-护送,4-其他
     */
    private Integer missionType;

    /**
     * 难度: 1-简单,2-普通,3-困难,4-地狱
     */
    private Integer difficulty;

    /**
     * 最低境界要求
     */
    private Integer minRealm;

    /**
     * 悬赏金额
     */
    private Long reward;

    /**
     * 发布者ID
     */
    private Long publisherId;

    /**
     * 接单者ID
     */
    private Long acceptorId;

    /**
     * 状态: 0-待接, 1-进行, 2-待验, 3-完成, 4-取消
     */
    private Integer status;

    /**
     * 取消原因
     */
    private String cancelReason;

    /**
     * 任务凭证 (JSON字符串)
     */
    private String proofData;

    /**
     * 截止时间
     */
    private LocalDateTime deadline;

    /**
     * 乐观锁
     */
    private Integer version;

    /**
     * 发布时间 / 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 揭榜时间 / 接单时间
     */
    private LocalDateTime acceptTime;
    /**
     * 交付时间 / 提交时间
     */
    private LocalDateTime submitTime;
    /**
     * 结单时间 / 验收时间
     */
    private LocalDateTime finishTime;
    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(Long publisherId) {
        this.publisherId = publisherId;
    }

    public Long getAcceptorId() {
        return acceptorId;
    }

    public void setAcceptorId(Long acceptorId) {
        this.acceptorId = acceptorId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getProofData() {
        return proofData;
    }

    public void setProofData(String proofData) {
        this.proofData = proofData;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getAcceptTime() {
        return acceptTime;
    }

    public void setAcceptTime(LocalDateTime acceptTime) {
        this.acceptTime = acceptTime;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}