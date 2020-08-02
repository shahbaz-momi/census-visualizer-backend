package com.beanz.censusviz.records

import javax.persistence.*

@Entity
@Table(name = "user_profiles")
data class DUserProfile(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        val uid: Int?,
        val username: String,
        val firstName: String,
        val lastName: String,
        val password_hash: String,
        val salt: String,
        val num_queries: Int,
        var dark_mode: Boolean
)