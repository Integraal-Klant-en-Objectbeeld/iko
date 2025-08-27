package com.ritense.iko.mvc.connector

data class ConnectorEditForm(
    val name: String,
    val description: String,
    val reference: String,
    val connectorCode: String,
)