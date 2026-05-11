package com.rag.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.rag.business.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface DocumentMapper extends BaseMapper<Document> {

    List<Long> selectAccessibleDocumentIds(@Param("userId") Long userId);
}
