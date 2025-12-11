package com.ritense.iko.aggregateddataprofile.camel

import org.springframework.data.domain.Pageable

data class ContainerParam(
    val containerId: String,
    val pageable: Pageable = Pageable.unpaged(),
    val filters: Map<String, String> = emptyMap(),
)