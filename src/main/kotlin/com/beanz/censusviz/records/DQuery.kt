package com.beanz.censusviz.records

import javax.persistence.*

@Entity
@Table(name = "saved_queries")
data class DQuery(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        val qid: Int? = null,
        val uid: Int,
        val last_updated: Long? = null,
        val query: String
)