package com.beanz.censusviz.records

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "geocode_lut")
data class DGeocode(
        @Id
        val geocode: String,
        val lon: Double,
        val lat: Double
)
