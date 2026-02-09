package com.li.lwg.service;

import com.li.lwg.dto.MissionAcceptReq;
import com.li.lwg.dto.MissionPublishReq;
import com.li.lwg.dto.MissionQueryReq;
import com.li.lwg.dto.MissionSubmitReq;
import com.li.lwg.entity.Mission;

import java.util.List;

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

    /**
     * 条件筛选查询任务
     *
     * @param req 筛选条件
     * @return 任务列
     */
    List<Mission> getMissionList(MissionQueryReq req);

    /**
     * 抢单
     *
     * @return 是否成功
     */
    boolean acceptMission(MissionAcceptReq req);

    /**
     * 提交任务
     *
     * @return 是否成功
     */
    boolean submitMission(MissionSubmitReq req);
}
