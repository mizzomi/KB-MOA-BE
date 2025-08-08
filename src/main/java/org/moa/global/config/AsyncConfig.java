package org.moa.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {

    @Bean(name = "ioTaskExecutor")
    public Executor ioTaskExecutor() {
        // I/O 작업은 대부분 대기 상태이므로, 스레드 개수를 넉넉하게 설정 (예: 50)
        return Executors.newFixedThreadPool(50);
    }
}