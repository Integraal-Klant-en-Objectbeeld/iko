package com.ritense.iko.mvc.model

data class CreateRelationRequest(
    val profileId: String = "",
    val name: String = "",
    // val primarySource: String,
    var transform: String = ""
)
