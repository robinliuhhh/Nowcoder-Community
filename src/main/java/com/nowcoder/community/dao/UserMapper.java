package com.nowcoder.community.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nowcoder.community.entity.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMapper extends BaseMapper<User> {
}
