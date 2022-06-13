package com.nowcoder.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nowcoder.community.entity.Comment;

public interface CommentService extends IService<Comment> {

    IPage<Comment> findCommentsByEntity(int entityType, int entityId, int current, int size);

    int findCommentCount(int entityType, int entityId);

    void addComment(Comment comment);

}
