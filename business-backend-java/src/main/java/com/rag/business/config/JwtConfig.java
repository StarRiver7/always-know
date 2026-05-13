package com.rag.business.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
// 绑定yml中的jwt配置
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private String secret;
    private Long expiration;
}
