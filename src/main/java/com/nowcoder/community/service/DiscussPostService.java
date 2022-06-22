package com.nowcoder.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nowcoder.community.entity.DiscussPost;

public interface DiscussPostService extends IService<DiscussPost> {

    IPage<DiscussPost> findDiscussPosts(int userId, int current, int size, int orderMode);

    int findDiscussPostRows(int userId);

    void addDiscussPost(DiscussPost post);

    void updateType(int id, int type);

    void updateStatus(int id, int status);

    void updateScore(int id, double score);

    void calculateDiscussPostScore(int discussPostId);

}
