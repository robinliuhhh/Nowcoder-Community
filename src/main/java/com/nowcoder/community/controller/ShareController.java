package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class ShareController implements CommunityConstant {

    @Value("${aliyun.endpoint}")
    private String endpoint;

    @Value("${aliyun.bucket-name}")
    private String bucketName;

    @Value("${aliyun.bucket-share-dir}")
    private String shareDir;

    // 生成分享图片是一个异步事件 放到Kafka队列 而不是直接等
    @Autowired
    private EventProducer eventProducer;

    @GetMapping("/share")
    @ResponseBody
    public String share(String htmlUrl) {
        // 文件名
        String fileName = shareDir + "/" + CommunityUtil.generateUUID();

        // 异步生成长图
        Event event = new Event()
                .setTopic(TOPIC_SHARE)
                .setData("htmlUrl", htmlUrl)
                .setData("fileName", fileName)
                .setData("suffix", ".png");
        eventProducer.fireEvent(event);

        // 返回访问路径
        Map<String, Object> map = new HashMap<>();
        String shareBucketUrl = "https://" + bucketName + "." + endpoint + "/" + fileName;
        map.put("shareUrl", shareBucketUrl);

        return CommunityUtil.getJSONString(0, null, map);
    }

}
