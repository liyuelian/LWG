package com.li.lwg.service;

import com.li.lwg.common.PageResult;
import com.li.lwg.entity.ReputationLog;

/**
 * @author liyuelian
 * @date 2026/2/18
 */
public interface ReputationService {
    /**
     * 分页查询我的信誉流水
     */
    PageResult<ReputationLog> getMyReputationLog(Integer page, Integer size, Long userId);
}