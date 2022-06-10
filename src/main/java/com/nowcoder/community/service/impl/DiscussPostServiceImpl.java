package com.nowcoder.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DiscussPostServiceImpl extends ServiceImpl<DiscussPostMapper, DiscussPost> implements DiscussPostService {

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Override
    public IPage<DiscussPost> findDiscussPosts(int userId, int current, int size) {
        QueryWrapper<DiscussPost> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(userId != 0, "userId", userId);
        queryWrapper.orderByDesc("type");
        queryWrapper.orderByDesc("create_time");
        return discussPostMapper.selectPage(new Page<>(current, size), queryWrapper);
    }

    @Override
    public int findDiscussPostRows(int userId) {
        QueryWrapper<DiscussPost> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(userId != 0, "userId", userId);
        return discussPostMapper.selectCount(queryWrapper).intValue();
    }

}
