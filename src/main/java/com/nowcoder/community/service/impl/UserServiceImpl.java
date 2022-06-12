package com.nowcoder.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService, CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    public Map<String, Object> register(User user) {
        Map<String, Object> msgMap = new HashMap<>();

        // 空值处理
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            // 业务漏洞 不是程序错误 不抛异常
            msgMap.put("usernameMsg", "账号不能为空!");
            return msgMap;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            msgMap.put("passwordMsg", "密码不能为空!");
            return msgMap;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            msgMap.put("emailMsg", "邮箱不能为空!");
            return msgMap;
        }

        // 验证账号
        QueryWrapper<User> usernameQW = new QueryWrapper<>();
        usernameQW.eq("username", user.getUsername());
        User u = userMapper.selectOne(usernameQW);
        if (u != null) {
            msgMap.put("usernameMsg", "该账号已存在!");
            return msgMap;
        }

        // 验证邮箱
        QueryWrapper<User> emailQW = new QueryWrapper<>();
        emailQW.eq("email", user.getEmail());
        u = userMapper.selectOne(emailQW);
        if (u != null) {
            msgMap.put("emailMsg", "该邮箱已被注册!");
            return msgMap;
        }

        // 注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insert(user);

        // 激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community/activation/userId/activation_code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);

        return msgMap;
    }

    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            user.setStatus(1);
            userMapper.updateById(user);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;
        }
    }

    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> msgMap = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(username)) {
            msgMap.put("usernameMsg", "账号不能为空!");
            return msgMap;
        }
        if (StringUtils.isBlank(password)) {
            msgMap.put("passwordMsg", "密码不能为空!");
            return msgMap;
        }

        // 验证账号
        QueryWrapper<User> usernameQW = new QueryWrapper<>();
        usernameQW.eq("username", username);
        User user = userMapper.selectOne(usernameQW);
        if (user == null) {
            msgMap.put("usernameMsg", "该账号不存在!");
            return msgMap;
        }

        // 验证状态
        if (user.getStatus() == 0) {
            msgMap.put("usernameMsg", "该账号未激活!");
            return msgMap;
        }

        // 验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            msgMap.put("passwordMsg", "密码不正确!");
            return msgMap;
        }

        // 生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        loginTicketMapper.insert(loginTicket);

        // ticket存入Session
        msgMap.put("ticket", loginTicket.getTicket());
        return msgMap;
    }

    public void logout(String ticket) {
        QueryWrapper<LoginTicket> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ticket", ticket);
        LoginTicket loginTicket = loginTicketMapper.selectOne(queryWrapper);
        loginTicket.setStatus(1);
        loginTicketMapper.update(loginTicket, queryWrapper);
    }

    public LoginTicket findLoginTicket(String ticket) {
        QueryWrapper<LoginTicket> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ticket", ticket);
        return loginTicketMapper.selectOne(queryWrapper);
    }

}
