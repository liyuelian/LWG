package com.li.lwg.controller;

import com.li.lwg.common.Result;
import com.li.lwg.dto.*;
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

    /**
     * 任务查询
     */
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

    /**
     * 审核结算接口 (发布者专用)
     */
    @PostMapping("/audit")
    public Result<String> audit(@RequestBody MissionAuditReq req) {
        // 调用 Service
        missionService.auditMission(req);

        // 根据审核结果返回不同的提示
        if (req.getPass()) {
            return Result.success("审核通过！赏金已发放，交易完成。");
        } else {
            return Result.success("审核驳回！已通知接单人重新修改凭证。");
        }
    }


    /**
     * 查询我的任务
     */
    @GetMapping("/my-missions")
    public Result<List<Mission>> getMyMissions(@RequestParam Long userId, @RequestParam Integer type) {
        // 简单参数校验
        if (type == null || (type != 1 && type != 2)) {
            return Result.error("参数错误：type 必须为 1(发布) 或 2(接取)");
        }
        return Result.success(missionService.getMyMissions(userId, type));
    }
}