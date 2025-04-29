package com.ritense.iko.mvc.model

import java.util.UUID

data class ModifyProfileRequest(
    val id: UUID,
    val name: String,
    // val primarySource: String,
    var transform: String
)
