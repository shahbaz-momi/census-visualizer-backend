package com.beanz.censusviz.records

import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "saved_queries")
data class DQuery(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        val qid: Int? = null,
        val uid: Int,
        val last_updated: Timestamp,
        val query: String
)