package com.ritense.iko.endpoints

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "endpoint")
class Endpoint(

    @Id
    val id: UUID,

    @Column
    var name: String,

    @Column
    var routeId: String,

    @Column(name = "created_on")
    var createdOn: LocalDateTime = LocalDateTime.now(),

    @Column(name = "modified_on")
    var modifiedOn: LocalDateTime? = null,

    @Column(name = "is_primary")
    var isPrimary: Boolean = false
)