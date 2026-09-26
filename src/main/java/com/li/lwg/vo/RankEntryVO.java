package com.li.lwg.vo;

import lombok.Data;

/** 公开榜单信息，不返回密码或钱包数据。 */
@Data
public class RankEntryVO {
    private Long rank;
    private Long userId;
    private String username;
    private Integer realm;
    private Long score;
}
