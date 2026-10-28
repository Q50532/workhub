package com.example.workhub.notification;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableScheduling
public class NotificationAsyncConfig {
    /** Fixed upper limits: no unbounded thread or task creation during request spikes. */
    @Bean("ticketNotificationExecutor")
    public ThreadPoolTaskExecutor ticketNotificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ticket-notify-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        // The default AbortPolicy throws on overload, allowing durable jobs to be retried later.
        executor.initialize();
        return executor;
    }
}
