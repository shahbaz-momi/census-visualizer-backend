package com.beanz.censusviz.records

import javax.persistence.*

@Entity
@Table(name = "user_profiles")
data class DUserProfile(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        val id: Int?,
        val username: String,
        val first_name: String,
        val last_name: String,
        val password_hash: String,
        val salt: String,
        val num_queries: Int
)