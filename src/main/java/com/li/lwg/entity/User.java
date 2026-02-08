package com.li.lwg.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author liyuelian
 * @date 2026/2/8
 * @desc 用户实体
 */
@Data
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
}