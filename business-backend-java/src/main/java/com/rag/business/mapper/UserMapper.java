package com.rag.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rag.business.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
