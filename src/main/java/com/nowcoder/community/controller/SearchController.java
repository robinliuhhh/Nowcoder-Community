package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    // search?keyword=xxx
    @GetMapping("/search")
    public String search(String keyword, Page page, Model model) {
        try {
            // 搜索帖子
            Map<String, Object> searchResult = elasticsearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());
            // 聚合数据
            List<Map<String, Object>> discussPosts = new ArrayList<>();
            List<DiscussPost> list = (List<DiscussPost>) searchResult.get("list");
            if (list != null) {
                for (DiscussPost post : list) {
                    Map<String, Object> map = new HashMap<>();
                    // 帖子
                    map.put("post", post);
                    // 作者
                    map.put("user", userService.getById(post.getUserId()));
                    // 点赞数量
                    map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));

                    discussPosts.add(map);
                }
            }
            model.addAttribute("discussPosts", discussPosts);
            model.addAttribute("keyword", keyword);

            // 分页信息
            page.setPath("/search?keyword=" + keyword);
            Long total = (Long) searchResult.get("total");
            page.setRows(total.intValue());
        } catch (IOException e) {
            logger.error("ES查询失败: " + e.getMessage());
        }
        return "/site/search";
    }

}
