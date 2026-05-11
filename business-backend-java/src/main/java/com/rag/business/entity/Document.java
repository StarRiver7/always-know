package com.rag.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rag_document")
public class Document {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String fileName;

    private String filePath;

    private Long fileSize;

    private String fileType;

    private Long userId;

    private Integer status;

    private Integer chunksProcessed;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
