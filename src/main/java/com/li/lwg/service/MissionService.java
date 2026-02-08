package com.li.lwg.service;

import com.li.lwg.dto.MissionPublishReq;

/**
 * @author liyuelian
 * @date 2026/2/8
 */
public interface MissionService {
    /**
     * 发布任务
     *
     * @return 发布成功的任务ID
     */
    Long publishMission(MissionPublishReq req);
}
