package com.nowcoder.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nowcoder.community.entity.DiscussPost;

public interface DiscussPostService extends IService<DiscussPost> {

    IPage<DiscussPost> findDiscussPosts(int userId, int current, int size);

    int findDiscussPostRows(int userId);

    int addDiscussPost(DiscussPost post);

}
