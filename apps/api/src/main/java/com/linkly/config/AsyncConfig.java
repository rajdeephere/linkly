package com.linkly.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Enables {@code @Async} and provides the small pool used to emit click events off the request thread. */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("clickExecutor")
    public Executor clickExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("click-");
        // Under a flood, drop rather than block the caller — analytics is best-effort.
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }
}
