/*
 * Copyright (C) 2026 Ritense BV, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ritense.iko

import com.ritense.iko.json.serializer.PageableDeserializer
import com.ritense.iko.json.serializer.PageableSerializer
import com.ritense.iko.route.RestConfigurationRoute
import org.apache.camel.CamelContext
import org.apache.camel.ProducerTemplate
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor

@Configuration
class ApplicationConfig {
    @Bean
    fun jsonCustomizer(): Jackson2ObjectMapperBuilderCustomizer = Jackson2ObjectMapperBuilderCustomizer { builder ->
        builder.serializerByType(Pageable::class.java, PageableSerializer())
        builder.deserializerByType(Pageable::class.java, PageableDeserializer())
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun restConfigurationRoute() = RestConfigurationRoute()

    @Bean
    fun producerTemplate(camelContext: CamelContext): ProducerTemplate = camelContext.createProducerTemplate()

    @Bean
    fun taskExecutor(): DelegatingSecurityContextAsyncTaskExecutor {
        val executor =
            ThreadPoolTaskExecutor().apply {
                maxPoolSize = 50
                threadNamePrefix = "security-task-executor-"
                initialize()
            }
        return DelegatingSecurityContextAsyncTaskExecutor(executor)
    }
}