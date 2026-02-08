package com.li.lwg.controller;

import com.li.lwg.common.Result;
import com.li.lwg.dto.MissionPublishReq;
import com.li.lwg.dto.MissionQueryReq;
import com.li.lwg.entity.Mission;
import com.li.lwg.service.MissionService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/list")
    public Result<List<Mission>> list(@RequestBody MissionQueryReq req) {
        List<Mission> list = missionService.getMissionList(req);
        return Result.success(list);
    }
}