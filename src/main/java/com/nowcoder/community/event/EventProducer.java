package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventProducer implements CommunityConstant {

    @Autowired
    private KafkaTemplate kafkaTemplate;

    /**
     * 发布事件
     */
    public void fireEvent(Event event) {
        // 将事件发布到指定的主题
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }

    /**
     * 帖子发生变化 -> 触发事件 -> elasticsearch存入最新数据
     */
    public void fireDiscussPostEvent(String topic, int userId, int discussPostId) {
        Event event = new Event()
                .setTopic(topic)
                .setUserId(userId)
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(discussPostId);
        fireEvent(event);
    }

}
