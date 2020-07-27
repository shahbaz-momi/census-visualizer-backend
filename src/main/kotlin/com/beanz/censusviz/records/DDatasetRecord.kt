package com.beanz.censusviz.records

import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class DDatasetRecord(
        @Id
        val id: Int,
        val age: Int,
        val sex: Int,
        val year: Int,
        val region: String,
        val geocode: String,
        val value: Int,
        val meta: Int
)