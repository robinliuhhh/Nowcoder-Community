package com.nowcoder.community.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nowcoder.community.entity.LoginTicket;
import org.springframework.stereotype.Repository;

@Repository
@Deprecated // LoginTicket不存MySQL数据库 改存Redis
public interface LoginTicketMapper extends BaseMapper<LoginTicket> {
}
