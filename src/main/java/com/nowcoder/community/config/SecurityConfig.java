package com.nowcoder.community.config;

import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;

import java.io.PrintWriter;

@Configuration
public class SecurityConfig implements CommunityConstant {

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // 忽略静态资源的访问
        return (web) -> web.ignoring().antMatchers("/resources/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // 授权
        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/discuss/add",
                        "/notice/**",
                        "/letter/**",
                        "/comment/add/**",
                        "/like",
                        "/follow",
                        "/unfollow")
                .hasAnyAuthority(AUTHORITY_USER, AUTHORITY_ADMIN, AUTHORITY_MODERATOR)
                .antMatchers(
                        "/discuss/top",
                        "/discuss/wonderful")
                .hasAnyAuthority(AUTHORITY_MODERATOR)
                .antMatchers(
                        "/discuss/delete",
                        "/data/**",
                        "/actuator/**")
                .hasAnyAuthority(AUTHORITY_ADMIN)
                .anyRequest().permitAll() // 除了上面的请求以外 其他所有请求都开放权限
                .and().csrf().disable(); // 禁用掉csrf验证（异步请求没有表单 需要在页面的<meta>标签和js中配置 很繁琐）

        // 在发生异常的时候如何处理 分同步请求（返回html）和异步请求（返回json）
        // 同步 or 异步 通过request消息头判断: request.getHeader("x-requested-with")
        http.exceptionHandling()
                // 未登录处理
                .authenticationEntryPoint((request, response, authException) -> {
//                    System.out.println("request = " + request.getHeader("x-requested-with"));
//                    System.out.println("context内容:" + SecurityContextHolder.getContext());
                    String requestHeader = request.getHeader("x-requested-with");
                    if ("XMLHttpRequest".equals(requestHeader)) {
                        response.setContentType("application/plain;charset=utf-8");
                        PrintWriter writer = response.getWriter();
                        // 异步请求处理
                        writer.write(CommunityUtil.getJSONString(403, "你还没有登录哦！"));
                    } else {
                        // 同步请求处理
                        response.sendRedirect(request.getContextPath() + "/login");
                    }
                })
                // 已登录但权限不足
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String requestHeader = request.getHeader("x-requested-with");
                    if ("XMLHttpRequest".equals(requestHeader)) {
                        response.setContentType("application/plain;charset=utf-8");
                        PrintWriter writer = response.getWriter();
                        writer.write(CommunityUtil.getJSONString(403, "你没有访问此功能的权限！"));
                    } else {
                        response.sendRedirect(request.getContextPath() + "/denied");
                    }
                });

        // Security底层默认会拦截/logout进行退出处理 覆盖它默认的逻辑 才能执行我们自己的退出代码
        // community-logout并不存在 只是为了让security拦截退出失效
        http.logout().logoutUrl("/community-logout");
        // Spring Security自带的认证是使用SecurityContext里面的认证结果来进行后续授权
        // 因为我们使用自定义login 所以需要手动把login认证的结果存到SecurityContext里面
        // Spring Security在DispatcherServlet之前拦截 DispatcherServlet在Controller之前
        // 因此可以用LoginTicketInterceptor拦截器进行存入
        return http.build();
    }

}
