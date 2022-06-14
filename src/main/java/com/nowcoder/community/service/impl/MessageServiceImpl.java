package com.nowcoder.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nowcoder.community.dao.MessageMapper;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    private List<Integer> findLatestMessageIds(int userId) {
        QueryWrapper<Message> subWrapper = new QueryWrapper<>();
        subWrapper.select("max(id) as id").ne("status", 2).ne("from_id", 1)
                .and(wrapper -> wrapper.eq("from_id", userId).or().eq("to_id", userId))
                .groupBy("conversation_id");
        List<Message> messageList = messageMapper.selectList(subWrapper);
        return messageList.stream().map(Message::getId).collect(Collectors.toList());
    }

    // 查询当前用户的会话列表 针对每个会话只返回一条最新的私信
    public IPage<Message> findConversations(int userId, int current, int size) {
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", findLatestMessageIds(userId)).orderByDesc("id");
        return messageMapper.selectPage(new Page<>(current, size), queryWrapper);
    }

    // 查询当前用户的会话数量
    public int findConversationCount(int userId) {
        return findLatestMessageIds(userId).size();
    }

    // 查询某个会话所包含的私信列表
    public IPage<Message> findLetters(String conversationId, int current, int size) {
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("status", 2).ne("from_id", 1).eq("conversation_id", conversationId)
                .orderByDesc("id");
        return messageMapper.selectPage(new Page<>(current, size), queryWrapper);
    }

    // 查询某个会话所包含的私信数量
    public int findLetterCount(String conversationId) {
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id")
                .ne("status", 2).ne("from_id", 1).eq("conversation_id", conversationId);
        return messageMapper.selectCount(queryWrapper).intValue();
    }

    // 查询未读私信的数量
    public int findLetterUnreadCount(int userId, String conversationId) {
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id")
                .eq("status", 0).ne("from_id", 1).eq("to_id", userId)
                .eq(conversationId != null, "conversation_id", conversationId);
        return messageMapper.selectCount(queryWrapper).intValue();
    }

    // 新增消息
    public void addMessage(Message message) {
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));
        message.setContent(sensitiveFilter.filter(message.getContent()));
        messageMapper.insert(message);
    }

    // 修改消息的状态
    public void readMessage(List<Integer> ids) {
        List<Message> messageList = messageMapper.selectBatchIds(ids);
        for (Message message : messageList) {
            message.setStatus(1);
            messageMapper.updateById(message);
        }
    }

    public void deleteMessage(int id) {
        Message message = messageMapper.selectById(id);
        message.setStatus(2);
        messageMapper.updateById(message);
    }

}
