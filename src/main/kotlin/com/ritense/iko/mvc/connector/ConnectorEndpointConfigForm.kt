package com.ritense.iko.mvc.connector

data class ConnectorEndpointConfigForm(
    val name: String,
    val description: String,
    val operation: String
)