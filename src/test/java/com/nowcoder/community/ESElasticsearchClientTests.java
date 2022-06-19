package com.nowcoder.community;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ESElasticsearchClientTests {

    private static final Logger logger = LoggerFactory.getLogger(ESElasticsearchClientTests.class);

    @Autowired
    private DiscussPostService discussPostService;

    private ElasticsearchClient client;

    @PostConstruct
    public void init() {
        // https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/connecting.html

        // Create the low-level client
        RestClient restClient = RestClient.builder(
                new HttpHost("localhost", 9200)).build();

        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        // And create the API client
        client = new ElasticsearchClient(transport);
    }

    @Test
    public void testBulkIndex() throws IOException {
        List<DiscussPost> posts = new ArrayList<>();
        List<Integer> ids = new ArrayList<>(Arrays.asList(101, 102, 103, 111, 112, 131, 132, 133, 134));
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (Integer postId : ids) {
            posts.addAll(discussPostService.findDiscussPosts(postId, 0, 100).getRecords());
        }
        for (DiscussPost post : posts) {
            br.operations(op -> op
                    .index(idx -> idx
                            .index("discusspost")
                            .id(String.valueOf(post.getId()))
                            .document(post)
                    )
            );
        }
        BulkResponse result = client.bulk(br.build());
        if (result.errors()) {
            logger.error("Bulk had errors");
            for (BulkResponseItem item : result.items()) {
                if (item.error() != null) {
                    logger.error(item.error().reason());
                }
            }
        }
    }

    // https://github.com/elastic/elasticsearch-java/blob/7.17/java-client/src/test/java/co/elastic/clients/documentation/usage/SearchingTest.java
    @Test
    public void testSearchByElasticsearchClient() throws IOException {
        SearchResponse<DiscussPost> response = client.search(s -> s
                .index("discusspost")
                .query(q -> q
                        .multiMatch(t -> t
                                .fields("title", "content")
                                .query("互联网寒冬")
                        ))
                .sort(o -> o
                        .field(f -> f
                                .field("type")
                                .order(SortOrder.Desc)))
                .sort(o -> o
                        .field(f -> f
                                .field("score")
                                .order(SortOrder.Desc)))
                .sort(o -> o
                        .field(f -> f
                                .field("createTime")
                                .order(SortOrder.Desc)))
                .highlight(h -> h
                        .fields("title", f -> f
                                .preTags("<span style='color:red'>")
                                .postTags("</span>"))
                        .fields("content", f -> f
                                .preTags("<em>")
                                .postTags("</em>")))
                .from(0).size(10), DiscussPost.class);
        TotalHits total = response.hits().total();
        boolean isExactResult = total.relation() == TotalHitsRelation.Eq;

        if (isExactResult) {
            logger.info("There are " + total.value() + " results");
        } else {
            logger.info("There are more than " + total.value() + " results");
        }

        Map<String, Object> res = new HashMap<>();
        List<DiscussPost> list = new ArrayList<>();
        for (Hit<DiscussPost> hit : response.hits().hits()) {
            DiscussPost post = hit.source();
            // title=<span style='color:red'>互联网</span>求职暖春计划
            if (hit.highlight().get("title") != null) {
                post.setTitle(hit.highlight().get("title").get(0));
            }
            // content=为了帮助大家度过&ldquo;<em>寒冬</em>&rdquo;，牛客网特别联合60+家企业，开启<em>互联网</em>求职暖春计划，面向18届&amp;19届，拯救0 offer！
            if (hit.highlight().get("content") != null) {
                post.setContent(hit.highlight().get("content").get(0));
            }
            System.out.println(post.toString());
            list.add(post);
        }
        res.put("list", list);
        res.put("total", total.value());
        System.out.println(res.toString());
    }

}
