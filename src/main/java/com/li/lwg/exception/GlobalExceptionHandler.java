package com.li.lwg.exception;

import com.li.lwg.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常捕获器
 * 作用：拦截所有 Controller 抛出的异常
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 拦截【业务异常】 (ServiceException)
     * 场景：余额不足、密码错误
     */
    @ExceptionHandler(ServiceException.class)
    public Result<?> handleServiceException(ServiceException e) {
        log.warn("业务异常拦截: {}", e.getMsg());
        return Result.error(e.getCode(), e.getMsg());
    }

    /**
     * 拦截【系统异常】 (Exception)
     * 场景：空指针、SQL语法错、数据库连不上
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统严重异常", e); // 打印堆栈信息方便排查
        return Result.error(500, "系统繁忙，请稍后再试");
    }
}