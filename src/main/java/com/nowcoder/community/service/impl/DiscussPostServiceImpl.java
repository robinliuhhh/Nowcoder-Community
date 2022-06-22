package com.nowcoder.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.util.RedisKeyUtil;
import com.nowcoder.community.util.SensitiveFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostServiceImpl extends ServiceImpl<DiscussPostMapper, DiscussPost> implements DiscussPostService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private RedisTemplate redisTemplate;

    // Caffeine核心接口: Cache, LoadingCache, AsyncLoadingCache

    // 帖子列表缓存
    private LoadingCache<String, List<DiscussPost>> postListCache;

    // 帖子总数缓存
    private LoadingCache<Integer, Integer> postRowsCache;

    @PostConstruct
    public void init() {
        // 初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(key -> {
                    if (key.length() == 0) {
                        throw new IllegalArgumentException("参数错误!");
                    }
                    String[] params = key.split(":");
                    if (params.length != 2) {
                        throw new IllegalArgumentException("参数错误!");
                    }

                    int current = Integer.parseInt(params[0]);
                    int size = Integer.parseInt(params[1]);

                    // todo 二级缓存: Redis -> mysql

                    logger.debug("load post list from DB.");
                    // orderMode只缓存热帖 按时间排序的帖子更新太快 不缓存
                    return selectDiscussPosts(0, current, size, 1);
                });
        // 初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(key -> {
                    logger.debug("load post rows from DB.");
                    return selectDiscussPostRows(key);
                });
    }

    private List<DiscussPost> selectDiscussPosts(int userId, int current, int size, int orderMode) {
        QueryWrapper<DiscussPost> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("status", 2).eq(userId != 0, "user_id", userId)
                .orderByDesc(orderMode == 0, "type", "create_time")
                .orderByDesc(orderMode == 1, "type", "score", "create_time");
        return discussPostMapper.selectPage(new Page<>(current, size), queryWrapper).getRecords();
    }

    @Override
    public List<DiscussPost> findDiscussPosts(int userId, int current, int size, int orderMode) {
        if (userId == 0 && orderMode == 1) {
            return postListCache.get(current + ":" + size);
        }
        logger.debug("load post list from DB.");

        return selectDiscussPosts(userId, current, size, orderMode);
    }

    private int selectDiscussPostRows(int userId) {
        QueryWrapper<DiscussPost> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("status", 2).eq("user_id", userId);
        return discussPostMapper.selectCount(queryWrapper).intValue();
    }

    @Override
    public int findDiscussPostRows(int userId) {
        if (userId == 0) {
            return postRowsCache.get(userId);
        }
        logger.debug("load post rows from DB.");

        return selectDiscussPostRows(userId);
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
