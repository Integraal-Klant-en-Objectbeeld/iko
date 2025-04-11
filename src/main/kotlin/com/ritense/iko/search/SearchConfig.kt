package com.ritense.iko.search

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SearchConfig {

    @Bean
    fun publicPersonenSearch() = PublicPersonenSearch()

    @Bean
    fun persoonSearch() = PersoonSearch()

    @Bean
    fun personenSearch() = PersonenSearch()

    @Bean
    fun postcodeEnHuisnummerPersonenSearch() = PostcodeEnHuisnummerPersonenSearch()

}