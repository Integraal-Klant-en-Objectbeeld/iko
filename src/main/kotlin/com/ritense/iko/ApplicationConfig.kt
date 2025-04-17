package com.ritense.iko

import com.ritense.iko.mvc.service.PersonClientService
import com.ritense.iko.route.RestConfigurationRoute
import org.apache.camel.CamelContext
import org.apache.camel.ConsumerTemplate
import org.apache.camel.FluentProducerTemplate
import org.apache.camel.ProducerTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order

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
    fun personClientService(
        fluentProducerTemplate: FluentProducerTemplate,
        consumerTemplate: ConsumerTemplate
    ) = PersonClientService(
        fluentProducerTemplate,
        consumerTemplate
    )
}