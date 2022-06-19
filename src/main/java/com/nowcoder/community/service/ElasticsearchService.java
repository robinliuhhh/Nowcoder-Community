package com.nowcoder.community.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.nowcoder.community.entity.DiscussPost;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElasticsearchService {

    @Value("${community.elasticsearch.hostname}")
    private String hostname;

    @Value("${community.elasticsearch.port}")
    private Integer port;

    private ElasticsearchClient client;

    @PostConstruct
    public void init() {
        // https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/connecting.html

        // Create the low-level client
        RestClient restClient = RestClient.builder(
                new HttpHost(hostname, port)).build();

        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        // And create the API client
        client = new ElasticsearchClient(transport);
    }

    public void saveDiscussPost(DiscussPost post) throws IOException {
        client.index(i -> i
                .index("discusspost")
                .id(String.valueOf(post.getId()))
                .document(post)
        );
    }

    public void deleteDiscussPost(int id) throws IOException {
        client.delete(d -> d
                .index("discusspost")
                .id(String.valueOf(id))
        );
    }

    public Map<String, Object> searchDiscussPost(String keyword, int current, int size) throws IOException {
        SearchResponse<DiscussPost> response = client.search(s -> s
                .index("discusspost")
                .query(q -> q
                        .multiMatch(t -> t
                                .fields("title", "content")
                                .query(keyword)
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
                .from(current).size(size), DiscussPost.class);

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

        Map<String, Object> res = new HashMap<>();
        res.put("list", list);
        res.put("total", response.hits().total().value());
        return res;
    }

}