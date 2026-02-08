package com.li.lwg.controller;

import com.li.lwg.common.Result;
import com.li.lwg.dto.MissionPublishReq;
import com.li.lwg.service.MissionService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mission")
public class MissionController {

    @Resource
    private MissionService missionService;

    /**
     * 发布悬赏任务
     */
    @PostMapping("/publish")
    public Result<Long> publish(@RequestBody MissionPublishReq req) {
        Long missionId = missionService.publishMission(req);
        return Result.success(missionId);
    }
}