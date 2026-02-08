package com.li.lwg.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author liyuelian
 * @date 2026/2/8
 * @desc 任务实体
 */
@Data
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
}