package com.ritense.iko.profile

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController("/profiles")
class ProfileController(private val profileRepository: ProfileRepository) {

    @GetMapping()
    fun getProfiles(): List<Profile> {
        return profileRepository.findAll()
    }

}