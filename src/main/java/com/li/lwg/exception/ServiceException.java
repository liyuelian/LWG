package com.li.lwg.exception;

/**
 * 业务逻辑异常
 * 比如：余额不足、任务不存在、权限不足
 * 继承 RuntimeException 是为了触发 @Transactional 回滚
 */
public class ServiceException extends RuntimeException {
    private Integer code;
    private String msg;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public ServiceException(String msg) {
        super(msg);
        this.code = 400;
        this.msg = msg;
    }


}