package com.nowcoder.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.util.Date;

@Setting(shards = 6, replicas = 3)
@Document(indexName = "discusspost")
@Data
public class DiscussPost {

    @Id
    @TableId(type = IdType.AUTO)
    private Integer id;

    @Field(type = FieldType.Integer)
    private int userId;

    // analyzer是存储时解析器 searchAnalyzer是搜索时的解析器
    // 比如要存"互联网校招" 存储分词时 要拆分出尽可能多的关键词以增加搜索范围 因此要用尽可能大的分词器ik_max_word: 互联|联网|网校|校招|互联网
    // 搜索的时候就没有必要分词这么精细 因此尽可能根据用户意图来分词 所以用ik_smart
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    @Field(type = FieldType.Integer)
    private int type;

    @Field(type = FieldType.Integer)
    private int status;

    // 如果不指定type = FieldType.Date es会把这个字段存为long型 -> "createTime": 1555424539000
    // type = FieldType.Date -> "createTime": "2019-04-16T14:22:19.000Z"
    @Field(type = FieldType.Date)
    private Date createTime;

    @Field(type = FieldType.Integer)
    private int commentCount;

    @Field(type = FieldType.Double)
    private double score;

}
