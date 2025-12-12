package com.ritense.iko.mvc.model.validation

import java.util.UUID

interface UniqueRelation {
    val aggregatedDataProfileId: UUID
    val id: UUID?
    val sourceId: UUID
    val propertyName: String
}