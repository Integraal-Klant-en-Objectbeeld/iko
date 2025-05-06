package com.ritense.iko.mvc.model

import java.util.UUID

data class EditRelationRequest(
    val profileId: UUID,
    val relationId: UUID,
    val sourceId: String,
    val searchId: String,
    val sourceToSearchMapping: String,
    var transform: String
)
