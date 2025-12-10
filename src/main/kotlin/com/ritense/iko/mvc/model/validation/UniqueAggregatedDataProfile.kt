package com.ritense.iko.mvc.model.validation

import java.util.UUID

interface UniqueAggregatedDataProfile {
    val id: UUID?
    val name: String
}