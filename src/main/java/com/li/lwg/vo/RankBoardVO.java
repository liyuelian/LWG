package com.li.lwg.vo;

import lombok.Data;
import java.util.List;

@Data
public class RankBoardVO {
    private String type;
    private Long total;
    private List<RankEntryVO> entries;
    // 未提供用户、用户不存在/被禁用或未达到上榜条件时为空。
    private RankEntryVO myRank;
}
