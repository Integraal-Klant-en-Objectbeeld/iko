package com.ritense.iko.mvc.service

import com.ritense.iko.search.PersonenSearch
import org.apache.camel.ConsumerTemplate
import org.apache.camel.FluentProducerTemplate

class PersonClientService(
    private val fluentProducerTemplate: FluentProducerTemplate,
    private val consumerTemplate: ConsumerTemplate
) {
    fun searchPersonByBSN(bsn: String): List<Any> {
        return fluentProducerTemplate.withHeader("bsn", bsn)
            .to(PersonenSearch.URI)
            .request(List::class.java) as List<Any>

    }

    fun getPersonByBSN(bsn: String): Any {
        return fluentProducerTemplate.withHeader("bsn", bsn)
            .to(PersonenSearch.URI)
            .request(Any::class.java)
    }
}

data class Person(var id: String, var name: String)