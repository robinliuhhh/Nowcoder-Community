package com.nowcoder.community.entity;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Event {

    private String topic;
    private int userId; // 触发者
    private int entityType;
    private int entityId;
    private int entityUserId; // 被触发的实体的作者
    // 处理其他事件存放的数据（因为还不知道有哪些属性 无法提前命名 故用map） 具有扩展性
    private Map<String, Object> data = new HashMap<>();

    public Event setTopic(String topic) {
        this.topic = topic;
        return this; // 返回当前对象 由此可以连续.调用其他set方法
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    // Map<String, Object> data -> String key, Object value
    public Event setData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

}
