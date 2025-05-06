package com.ritense.iko.mvc.model

data class AddProfileForm(
    val name: String,
    val primarySource: String,
    var transform: String
)
