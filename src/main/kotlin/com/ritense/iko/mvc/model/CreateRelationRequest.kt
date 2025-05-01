package com.ritense.iko.mvc.model

import java.util.UUID

data class CreateRelationRequest(
    val profileId: UUID,
    val sourceId: String,
    val searchId: String,
    val sourceToSearchMapping: String,
    var transform: String
)
