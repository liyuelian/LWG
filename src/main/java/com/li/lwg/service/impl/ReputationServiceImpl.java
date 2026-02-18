package com.li.lwg.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.li.lwg.common.PageResult;
import com.li.lwg.entity.ReputationLog;
import com.li.lwg.service.ReputationService;
import com.li.lwg.mapper.ReputationLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author liyuelian
 * @date 2026/2/18
 */
@Service
public class ReputationServiceImpl implements ReputationService {

    @Resource
    private ReputationLogMapper reputationLogMapper;

    @Override
    public PageResult<ReputationLog> getMyReputationLog(Integer page, Integer size, Long userId) {
        PageHelper.startPage(page, size);

        // 执行查询
        List<ReputationLog> list = reputationLogMapper.selectByUserId(userId);

        PageInfo<ReputationLog> info = new PageInfo<>(list);
        return new PageResult<>(info.getTotal(), info.getList());
    }
}
