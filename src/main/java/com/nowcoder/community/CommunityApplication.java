package com.nowcoder.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class CommunityApplication {

//    // @PostConstruct 当容器实例化当前bean以后（调用CommunityApplication的构造方法之后） 该方法自动调用
//    @PostConstruct
//    public void init() {
//        // 解决netty启动冲突问题
//        // redis 和 es 都依赖于netty 当redis启动后es会检查netty 发现已设置则会不启动 因此报错
//        // see Netty4Utils.setAvailableProcessors()
//        System.setProperty("es.set.netty.runtime.available.processors", "false");
//    }

    public static void main(String[] args) {
        SpringApplication.run(CommunityApplication.class, args);
    }

}
