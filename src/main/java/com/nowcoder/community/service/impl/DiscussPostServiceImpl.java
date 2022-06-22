package com.nowcoder.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.util.RedisKeyUtil;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class DiscussPostServiceImpl extends ServiceImpl<DiscussPostMapper, DiscussPost> implements DiscussPostService {

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public IPage<DiscussPost> findDiscussPosts(int userId, int current, int size, int orderMode) {
        QueryWrapper<DiscussPost> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("status", 2).eq(userId != 0, "user_id", userId)
                .orderByDesc(orderMode == 0, "type", "create_time")
                .orderByDesc(orderMode == 1, "type", "score", "create_time");
        return discussPostMapper.selectPage(new Page<>(current, size), queryWrapper);
    }

    @Override
    public int findDiscussPostRows(int userId) {
        QueryWrapper<DiscussPost> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("status", 2).eq(userId != 0, "user_id", userId);
        return discussPostMapper.selectCount(queryWrapper).intValue();
    }

    public void addDiscussPost(DiscussPost post) {
        if (post == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        // 转义HTML标记 防止破坏页面
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        // 过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        discussPostMapper.insert(post);
    }

    public void updateType(int id, int type) {
        DiscussPost discussPost = discussPostMapper.selectById(id);
        discussPost.setType(type);
        discussPostMapper.updateById(discussPost);
    }

    public void updateStatus(int id, int status) {
        DiscussPost discussPost = discussPostMapper.selectById(id);
        discussPost.setStatus(status);
        discussPostMapper.updateById(discussPost);
    }

    public void updateScore(int id, double score) {
        DiscussPost discussPost = discussPostMapper.selectById(id);
        discussPost.setScore(score);
        discussPostMapper.updateById(discussPost);
    }

    /**
     * 计算帖子分数
     */
    public void calculateDiscussPostScore(int discussPostId) {
        // 放入Set去重
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, discussPostId);
    }

}
