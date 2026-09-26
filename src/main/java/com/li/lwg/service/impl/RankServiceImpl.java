package com.li.lwg.service.impl;

import com.li.lwg.enums.RankTypeEnum;
import com.li.lwg.exception.ServiceException;
import com.li.lwg.mapper.RankMapper;
import com.li.lwg.service.RankService;
import com.li.lwg.vo.RankBoardVO;
import com.li.lwg.vo.RankEntryVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RankServiceImpl implements RankService {
    @Resource
    private RankMapper rankMapper;
    @Resource
    private RankIndex rankIndex;

    @Override
    public RankBoardVO getBoard(String type, int limit, Long userId) {
        RankTypeEnum rankType = RankTypeEnum.fromCode(type);
        if (limit < 1 || limit > 100) {
            throw new ServiceException("榜单人数须在 1 到 100 之间");
        }
        if (userId != null && userId <= 0) {
            throw new ServiceException("道友 ID 必须为正整数");
        }

        List<RankEntryVO> ranked = rankIndex.top(rankType, limit);
        RankEntryVO personal = userId == null ? null : rankIndex.personal(rankType, userId);
        List<Long> userIds = new ArrayList<>();
        for (RankEntryVO entry : ranked) {
            userIds.add(entry.getUserId());
        }
        if (personal != null && !userIds.contains(personal.getUserId())) {
            userIds.add(personal.getUserId());
        }
        Map<Long, RankEntryVO> profiles = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (RankEntryVO profile : rankMapper.selectProfiles(userIds)) {
                profiles.put(profile.getUserId(), profile);
            }
        }
        List<RankEntryVO> entries = new ArrayList<>();
        for (RankEntryVO entry : ranked) {
            if (profiles.containsKey(entry.getUserId())) {
                applyProfile(entry, profiles.get(entry.getUserId()));
                entries.add(entry);
            }
        }
        if (personal != null && profiles.containsKey(personal.getUserId())) {
            applyProfile(personal, profiles.get(personal.getUserId()));
        } else {
            personal = null;
        }

        RankBoardVO board = new RankBoardVO();
        board.setType(rankType.getCode());
        board.setTotal(rankIndex.total(rankType));
        board.setEntries(entries);
        board.setMyRank(personal);
        return board;
    }

    private void applyProfile(RankEntryVO entry, RankEntryVO profile) {
        entry.setUsername(profile.getUsername());
        entry.setRealm(profile.getRealm());
    }
}
