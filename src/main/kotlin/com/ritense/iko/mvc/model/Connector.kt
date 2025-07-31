package com.ritense.iko.mvc.model

/**
 * Simple data class representing a connector entry in the admin UI.
 *
 * @param id unique identifier used in URLs and lookup
 * @param name human‑friendly display name
 * @param description short description shown in the list view
 * @param route optional route used for linking
 */
data class Connector(
    val id: String,
    val name: String,
    val description: String,
    val route: String,
)
