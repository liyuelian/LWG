package com.li.lwg.entity;

import java.time.LocalDateTime;

/**
 * @author liyuelian
 * @date 2026/2/8
 * @desc 用户实体
 */
public class User {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 道号
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 境界
     */
    private Integer realm;

    /**
     * 可用灵石 (单位:分)
     */
    private Long balance;

    /**
     * 冻结灵石 (单位:分)
     */
    private Long frozenBalance;

    /**
     * 状态: 0-禁用, 1-正常
     */
    private Integer status;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getRealm() {
        return realm;
    }

    public void setRealm(Integer realm) {
        this.realm = realm;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public Long getFrozenBalance() {
        return frozenBalance;
    }

    public void setFrozenBalance(Long frozenBalance) {
        this.frozenBalance = frozenBalance;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}