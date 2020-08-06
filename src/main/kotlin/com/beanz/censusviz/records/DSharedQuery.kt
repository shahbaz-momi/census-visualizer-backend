package com.beanz.censusviz.records

import org.springframework.boot.context.properties.ConstructorBinding
import java.io.Serializable
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.Table

@Entity
@IdClass(SharedQueryId::class)
@Table(name = "shared_queries")
data class DSharedQuery(
        @Id
        val qid: Int,
        @Id
        val uid: Int
)

data class SharedQueryId(
        val qid: Int,
        val uid: Int
): Serializable