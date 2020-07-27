package com.beanz.censusviz.repos

import com.beanz.censusviz.records.DDatasetCombinedRecord
import com.beanz.censusviz.records.DDatasetRecord
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import javax.persistence.Table

@Table(name = "education")
interface DatasetEducationRepo : CrudRepository<DDatasetRecord, Int> {

    @Query("SELECT SUM(value) as count, lat, lon FROM education NATURAL JOIN geocode_lut WHERE meta IN :metas AND age IN :ages AND sex = :sex GROUP BY geocode, lon, lat", nativeQuery = true)
    fun findAllByAgeInAndSexAndMetaIn(@Param("ages") ages: List<Int>, @Param("sex") sex: Int, @Param("metas") metas: List<Int>): List<DDatasetCombinedRecord>

}