package com.beanz.censusviz.repos

import com.beanz.censusviz.records.DUserProfile
import org.springframework.data.repository.CrudRepository
import javax.persistence.Table

@Table(name = "user_profiles")
interface UserProfileRepo : CrudRepository<DUserProfile, Int> {

    fun findByUsername(username: String): DUserProfile?

}