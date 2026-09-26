package com.li.lwg.service;

import com.li.lwg.vo.RankBoardVO;

public interface RankService {
    RankBoardVO getBoard(String type, int limit, Long userId);
}
