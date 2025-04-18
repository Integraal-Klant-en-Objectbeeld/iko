package com.ritense.iko.profile

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping(path = ["/test"])
@RestController
class TestController(val profileService: ProfileService, val profileRepository: ProfileRepository) {

    @GetMapping
    fun test() {
        val profile = profileRepository.findAll().get(0);

        profileService.removeRoutes(profile);
        profile.name = "TEST";
        profileService.addRoutes(profile);
    }

}