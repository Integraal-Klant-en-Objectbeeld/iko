package com.ritense.iko.connectors.openklant

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI

@ConfigurationProperties(prefix = "iko.connectors.openklant")
data class OpenKlantProperties(
    val enabled: Boolean = false,
    val instances: Map<String, OpenKlantInstanceProperties> = emptyMap(),
    // For backward compatibility with existing configuration
    val host: String? = null,
    val specificationUri: URI? = null,
    val token: String? = null
)

data class OpenKlantInstanceProperties(
    val host: String,
    val specificationUri: URI,
    val token: String
)