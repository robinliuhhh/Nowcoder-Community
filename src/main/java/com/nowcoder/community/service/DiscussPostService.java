package com.nowcoder.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nowcoder.community.entity.DiscussPost;

import java.util.List;

public interface DiscussPostService extends IService<DiscussPost> {

    List<DiscussPost> findDiscussPosts(int userId, int current, int size, int orderMode);

    int findDiscussPostRows(int userId);

    void addDiscussPost(DiscussPost post);

    void updateType(int id, int type);

    void updateStatus(int id, int status);

    void updateScore(int id, double score);

    void calculateDiscussPostScore(int discussPostId);

}
