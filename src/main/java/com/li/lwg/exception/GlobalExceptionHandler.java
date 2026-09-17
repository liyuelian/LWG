package com.li.lwg.exception;

import com.li.lwg.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
     * 拦截【参数校验异常】 (MethodArgumentNotValidException)
     * 场景：@RequestBody 缺少必填字段、或字段超出长度限制
     *
     * <p>若不单独处理会落到下面的 Exception 兜底分支返回 500，
     * 调用方看到"系统繁忙"却不知道是自己参数传错了，排查成本很高。
     * 这里返回 400 并附带第一条校验提示。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("请求参数不合法");
        log.warn("参数校验失败: {}", msg);
        return Result.error(400, msg);
    }

    /**
     * 拦截【数据完整性异常】 (DataIntegrityViolationException)
     * 场景：字段超长、唯一键冲突、违反非空约束
     *
     * <p>这类问题本质是传入数据不合法或与现有数据冲突，属于业务性错误，
     * 应返回 400 而不是 500，避免把可修复的输入问题伪装成系统故障。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<?> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("数据完整性校验失败: {}", e.getMostSpecificCause().getMessage());
        return Result.error(400, "数据不合法或与现有数据冲突，请检查后重试");
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