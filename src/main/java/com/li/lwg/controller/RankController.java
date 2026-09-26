package com.li.lwg.controller;

import com.li.lwg.common.Result;
import com.li.lwg.service.RankService;
import com.li.lwg.vo.RankBoardVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/rank")
public class RankController {
    @Resource
    private RankService rankService;

    @GetMapping("/board")
    public Result<RankBoardVO> getBoard(
            @RequestParam(defaultValue = "reputation") String type,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) Long userId) {
        return Result.success(rankService.getBoard(type, limit, userId));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleInvalidNumber(MethodArgumentTypeMismatchException exception) {
        return Result.error("榜单人数和道友 ID 必须为有效整数");
    }
}
