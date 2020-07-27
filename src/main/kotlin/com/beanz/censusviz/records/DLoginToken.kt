package com.beanz.censusviz.records

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "login_tokens")
data class DLoginToken(
        @Id
        val token: String,
        val username: String
)