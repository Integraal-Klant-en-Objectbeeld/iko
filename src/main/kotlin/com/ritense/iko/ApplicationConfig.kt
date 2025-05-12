package com.ritense.iko

import com.ritense.iko.route.RestConfigurationRoute
import org.apache.camel.CamelContext
import org.apache.camel.ProducerTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor
import org.springframework.security.task.DelegatingSecurityContextTaskExecutor


@Configuration
class ApplicationConfig() {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun restConfigurationRoute() = RestConfigurationRoute()

    @Bean
    fun producerTemplate(camelContext: CamelContext): ProducerTemplate {
        return camelContext.createProducerTemplate()
    }

    @Bean
    fun taskExecutor(): DelegatingSecurityContextAsyncTaskExecutor {
        val executor = ThreadPoolTaskExecutor().apply {
            corePoolSize = 50
            maxPoolSize = 100
            threadNamePrefix = "async-executor-"
            initialize()
        }
        return DelegatingSecurityContextAsyncTaskExecutor(executor)
    }

}