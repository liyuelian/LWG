package com.li.lwg.controller;

import com.li.lwg.common.Result;
import com.li.lwg.dto.MissionAcceptReq;
import com.li.lwg.dto.MissionPublishReq;
import com.li.lwg.dto.MissionQueryReq;
import com.li.lwg.dto.MissionSubmitReq;
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

    /**
     * 抢单接口
     */
    @PostMapping("/accept")
    public Result<String> accept(@RequestBody MissionAcceptReq req) {
        missionService.acceptMission(req);
        return Result.success("抢单成功，请尽快完成任务！");
    }

    /**
     * 交付任务接口
     */
    @PostMapping("/submit")
    public Result<String> submit(@RequestBody MissionSubmitReq req) {
        missionService.submitMission(req);
        return Result.success("任务交付成功，请等待发布者验收结算！");
    }
}