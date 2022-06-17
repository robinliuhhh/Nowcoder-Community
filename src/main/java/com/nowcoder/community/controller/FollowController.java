package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements CommunityConstant {

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private EventProducer eventProducer;

    @LoginRequired
    @PostMapping("/follow")
    @ResponseBody
    public String follow(int entityType, int entityId) {
        User user = hostHolder.getUser();

        followService.follow(user.getId(), entityType, entityId);

        // 触发关注事件
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0, "已关注!");
    }

    @LoginRequired
    @PostMapping("/unfollow")
    @ResponseBody
    public String unFollow(int entityType, int entityId) {
        User user = hostHolder.getUser();

        followService.unFollow(user.getId(), entityType, entityId);

        return CommunityUtil.getJSONString(0, "已取消关注!");
    }

    @GetMapping("/followees/{userId}")
    public String getFollowees(@PathVariable("userId") int userId, Page page, Model model) {
        getFollows(QUERY_TYPE_FOLLOWEE, userId, page, model);
        return "/site/followee";
    }

    @GetMapping("/followers/{userId}")
    public String getFollowers(@PathVariable("userId") int userId, Page page, Model model) {
        getFollows(QUERY_TYPE_FOLLOWER, userId, page, model);
        return "/site/follower";
    }

    private void getFollows(int usage, int userId, Page page, Model model) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user", user);

        page.setLimit(5);
        page.setPath((usage == QUERY_TYPE_FOLLOWEE ? "/followees/" : "/followers/") + userId);
        int rows = (int) (usage == QUERY_TYPE_FOLLOWEE ? followService.findFolloweeCount(userId, ENTITY_TYPE_USER) :
                followService.findFollowerCount(ENTITY_TYPE_USER, userId));
        page.setRows(rows);

        List<Map<String, Object>> userList = (usage == QUERY_TYPE_FOLLOWEE ?
                followService.findFollowees(userId, page.getOffset(), page.getLimit()) :
                followService.findFollowers(userId, page.getOffset(), page.getLimit()));
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users", userList);
    }

    private boolean hasFollowed(int userId) {
        if (hostHolder.getUser() == null) {
            return false;
        }

        return followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
    }

}
