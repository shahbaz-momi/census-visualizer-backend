package com.beanz.censusviz.records

import org.springframework.boot.context.properties.ConstructorBinding
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "saved_queries")
@ConstructorBinding
data class DQuery(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        val qid: Int? = null,
        val uid: Int,
        val last_updated: Timestamp,
        val query: String
)