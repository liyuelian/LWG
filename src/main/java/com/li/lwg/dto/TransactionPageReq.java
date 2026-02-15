package com.li.lwg.dto;

import java.io.Serializable;
import java.util.List;

/**
 * @author liyuelian
 * @date 2026/2/15
 * 流水查询入参
 */
public class TransactionPageReq implements Serializable {
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 分页
     */
    private Integer page = 1;
    /**
     * 每页大小
     */
    private Integer pageSize = 10;
    /**
     * 查询开始时间 (格式: yyyy-MM-dd)
     */
    private String startDate;
    /**
     * 查询结束时间 (格式: yyyy-MM-dd)
     */
    private String endDate;
    /**
     * 筛选维度: "all"=全部, "income"=只看收入, "expense"=只看支出
     */
    private String category;

    private List<Integer> typeList;

    // 偏移量
    public int getOffset() {
        return (page - 1) * pageSize;
    }

    public List<Integer> getTypeList() {
        return typeList;
    }

    public void setTypeList(List<Integer> typeList) {
        this.typeList = typeList;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
