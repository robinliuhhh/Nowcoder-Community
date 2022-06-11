package com.nowcoder.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nowcoder.community.entity.User;

import java.util.Map;

public interface UserService extends IService<User> {

    Map<String, Object> register(User user);

    int activation(int userId, String code);

}
