package com.beanz.censusviz.repos

import com.beanz.censusviz.records.DDatasetCombinedRecord
import com.beanz.censusviz.records.DDatasetDoubleCombinedRecord
import com.beanz.censusviz.records.DDatasetRecord
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import javax.persistence.Table

@Table(name = "education")
interface DatasetPopulationRepo : CrudRepository<DDatasetRecord, Int> {

    @Query("SELECT SUM(value) as count, lat, lon FROM population NATURAL JOIN geocode_lut WHERE age IN :ages AND sex = :sex GROUP BY geocode, lon, lat", nativeQuery = true)
    fun findAllByAgeInAndSex(@Param("ages") ages: List<Int>, @Param("sex") sex: Int): List<DDatasetDoubleCombinedRecord>

}