package com.ritense.iko

import com.ritense.iko.route.RestConfigurationRoute
import org.apache.camel.CamelContext
import org.apache.camel.ProducerTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor

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
            maxPoolSize = 50
            threadNamePrefix = "security-task-executor-"
            initialize()
        }
        return DelegatingSecurityContextAsyncTaskExecutor(executor)
    }

}