package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.PutObjectRequest;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Value("${wk.image.command}")
    private String wkImageCommand;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${aliyun.endpoint}")
    private String endpoint;

    @Value("${aliyun.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.bucket-name}")
    private String bucketName;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record) {
        Event event = checkMessage(record);

        // 发送站内通知
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        // 页面展示数据
        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());

        if (!event.getData().isEmpty()) {
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue());
            }
        }

        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);
    }

    // 消费发帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record) throws IOException {
        Event event = checkMessage(record);
        DiscussPost post = discussPostService.getById(event.getEntityId());
        elasticsearchService.saveDiscussPost(post);
    }

    // 消费删帖事件
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record) throws IOException {
        Event event = checkMessage(record);
        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }

    private Event checkMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            throw new IllegalArgumentException("消息的内容为空!");
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            throw new IllegalArgumentException("消息格式错误!");
        }

        return event;
    }

    // 消费分享事件
    @KafkaListener(topics = TOPIC_SHARE)
    public void handleShareMessage(ConsumerRecord record) {
        Event event = checkMessage(record);

        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");

        String cmd = wkImageCommand + " --quality 75 "
                + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            logger.error("生成长图失败: " + e.getMessage());
        }

        // 启用定时器监视该图片 一旦生成了则上传至阿里云OSS
        // 因为这个是消息队列 所以可以不用quartz
        // 不用担心ThreadPoolTaskScheduler冲突问题 因为分布式部署时不论多少台服务器 最终只有一个Consumer能抢到任务
        UploadTask task = new UploadTask(fileName, suffix);
        // 0.5s检查一次
        Future future = taskScheduler.scheduleAtFixedRate(task, 500);
        // 实例化UploadTask之后设置Future（定时器任务）
        task.setFuture(future);
    }

    // 别的地方用不到 写成内部类即可
    class UploadTask implements Runnable {
        // 文件名称
        private String fileName;
        // 文件后缀
        private String suffix;
        // 启动任务的返回值
        private Future future;
        // 开始时间
        private long startTime;
        // 上传次数 正常情况下1次就成功 次数多说明上传至阿里云失败
        private int uploadTimes;

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run() {
            // 防止任务失败 持续消耗服务器资源
            // 生成失败 超过30s
            if (System.currentTimeMillis() - startTime > 30000) {
                logger.error("执行时间过长 终止任务: " + fileName);
                future.cancel(true);
                return;
            }
            // 上传失败
            if (uploadTimes >= 3) {
                logger.error("上传次数过多 终止任务: " + fileName);
                future.cancel(true);
                return;
            }

            // 先上传至本地
            String path = wkImageStorage + "/" + fileName + suffix;
            File file = new File(path);
            if (file.exists()) {
                logger.info(String.format("开始第%d次上传[%s].", ++uploadTimes, fileName));
                // 创建OSSClient实例
                OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
                try {
                    PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, file);
                    // 上传文件
                    ossClient.putObject(putObjectRequest);
                    logger.info(String.format("第%d次上传成功[%s].", uploadTimes, fileName));
                    future.cancel(true);
                } catch (OSSException e) {
                    logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                } finally {
                    if (ossClient != null) {
                        ossClient.shutdown();
                    }
                }
            } else {
                logger.info("等待图片生成[" + fileName + "].");
            }
        }
    }

}
