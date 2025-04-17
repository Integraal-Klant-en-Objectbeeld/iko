package com.ritense.iko.profile

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProfileRepository : JpaRepository<Profile, UUID> {
}