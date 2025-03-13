package com.ritense.iko

import org.apache.camel.ProducerTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class IkoApplication() {
}

fun main(args: Array<String>) {
    runApplication<IkoApplication>(*args)
}
