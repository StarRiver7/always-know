package com.rag.business;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.rag.business.mapper")
public class RagBusinessApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagBusinessApplication.class, args);
    }
}
