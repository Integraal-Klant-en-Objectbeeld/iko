package com.ritense.iko.mvc.model

data class Endpoint(
    val id: String,
    val name: String,
    val isPrimary: Boolean,
    val isActive: Boolean,
)