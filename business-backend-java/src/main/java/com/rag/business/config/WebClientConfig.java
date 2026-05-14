package com.rag.business.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient配置类
 *
 * @author RAG Business Team
 * @since 2026-05-13
 */
@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${ai.backend.url}")
    private String aiBackendUrl;

    @Value("${ai.backend.timeout.connect:5000}")
    private int connectTimeout;

    @Value("${ai.backend.timeout.read:60000}")
    private int readTimeout;

    @Value("${ai.backend.timeout.write:60000}")
    private int writeTimeout;

    @Bean
    public WebClient aiBackendWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS)))
                .responseTimeout(Duration.ofMillis(readTimeout));

        return WebClient.builder()
                .baseUrl(aiBackendUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) ->
                    values.forEach(value -> log.info("{}: {}", name, value)));
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("Response Status: {}", clientResponse.statusCode());
            if (clientResponse.statusCode().isError()) {
                return clientResponse.createException()
                        .flatMap(ex -> {
                            log.error("Request failed: {} {}", clientResponse.statusCode(), ex.getMessage());
                            return Mono.just(clientResponse);
                        });
            }
            return Mono.just(clientResponse);
        });
    }
}
