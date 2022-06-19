package com.nowcoder.community;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ESRestHighLevelClientTests {

    @Autowired
    private DiscussPostMapper discussMapper;

    @Autowired
    private DiscussPostRepository discussRepository;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Test
    public void testInsert() {
        discussRepository.save(discussMapper.selectById(241));
        discussRepository.save(discussMapper.selectById(242));
        discussRepository.save(discussMapper.selectById(243));
    }

    @Test
    public void testInsertList() {
        List<DiscussPost> posts = new ArrayList<>();
        List<Integer> ids = new ArrayList<>(Arrays.asList(101, 102, 103, 111, 112, 131, 132, 133, 134));
        for (Integer postId : ids) {
            posts.addAll(discussPostService.findDiscussPosts(postId, 0, 100).getRecords());
        }
        discussRepository.saveAll(posts);
//        discussRepository.saveAll(discussPostService.findDiscussPosts(101, 0, 100).getRecords());
//        discussRepository.saveAll(discussPostService.findDiscussPosts(102, 0, 100).getRecords());
//        discussRepository.saveAll(discussPostService.findDiscussPosts(103, 0, 100).getRecords());
//        discussRepository.saveAll(discussPostService.findDiscussPosts(111, 0, 100).getRecords());
//        discussRepository.saveAll(discussPostService.findDiscussPosts(112, 0, 100).getRecords());
//        discussRepository.saveAll(discussPostService.findDiscussPosts(131, 0, 100).getRecords());
//        discussRepository.saveAll(discussPostService.findDiscussPosts(132, 0, 100).getRecords());
//        discussRepository.saveAll(discussPostService.findDiscussPosts(133, 0, 100).getRecords());
//        discussRepository.saveAll(discussPostService.findDiscussPosts(134, 0, 100).getRecords());
    }

    @Test
    public void testUpdate() {
        DiscussPost post = discussMapper.selectById(231);
        post.setContent("我是新人,使劲灌水.");
        discussRepository.save(post);
    }

    @Test
    public void testDelete() {
        // discussRepository.deleteById(231);
        discussRepository.deleteAll();
    }

    // https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high-search.html
    @Test
    public void testSearchByRestHighLevelClient() throws IOException {
        SearchRequest searchRequest = new SearchRequest("discusspost");

        // 高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.field("content");
        highlightBuilder.requireFieldMatch(true);
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");

        // 构建搜索条件
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                // 在discusspost索引的title和content字段中都查询“互联网寒冬”
                .query(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                // matchQuery是模糊查询 会对key进行分词 searchSourceBuilder.query(QueryBuilders.matchQuery(key,value));
                // termQuery是精准查询 searchSourceBuilder.query(QueryBuilders.termQuery(key,value));
                .sort(SortBuilders.fieldSort("type").order(SortOrder.DESC)) // 置顶
                .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC)) // 加精
                .sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                // 一个可选项 用于控制允许搜索的时间 searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
                .from(0) // 指定从哪条开始查询
                .size(10) // 需要查出的总记录条数
                .highlighter(highlightBuilder);
        // Add the SearchSourceBuilder to the SearchRequest.
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        Map<String, Object> res = new HashMap<>();
        List<DiscussPost> list = new LinkedList<>();
        long total = searchResponse.getHits().getTotalHits().value;
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            DiscussPost discussPost = JSONObject.parseObject(hit.getSourceAsString(), DiscussPost.class);

            // 处理高亮显示的结果
            HighlightField titleField = hit.getHighlightFields().get("title");
            if (titleField != null) {
                discussPost.setTitle(titleField.getFragments()[0].toString());
            }
            HighlightField contentField = hit.getHighlightFields().get("content");
            if (contentField != null) {
                discussPost.setContent(contentField.getFragments()[0].toString());
            }
            list.add(discussPost);
        }
        res.put("list", list);
        res.put("total", total);
        if (res.get("list") != null) {
            for (DiscussPost post : (List<DiscussPost>) res.get("list")) {
                System.out.println(post);
            }
            System.out.println("total: " + res.get("total"));
        }
    }

}
