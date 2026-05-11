package com.rag.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rag.business.entity.ChatHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {
}
