package com.nowcoder.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nowcoder.community.entity.Message;

import java.util.List;

public interface MessageService extends IService<Message> {

    IPage<Message> findConversations(int userId, int current, int size);

    int findConversationCount(int userId);

    IPage<Message> findLetters(String conversationId, int current, int size);

    int findLetterCount(String conversationId);

    int findLetterUnreadCount(int userId, String conversationId);

    void addMessage(Message message);

    void readMessage(List<Integer> ids);

    void deleteMessage(int id);

    Message findLatestNotice(int userId, String topic);

    int findNoticeCount(int userId, String topic);

    int findNoticeUnreadCount(int userId, String topic);

    IPage<Message> findNotices(int userId, String topic, int current, int size);

}
