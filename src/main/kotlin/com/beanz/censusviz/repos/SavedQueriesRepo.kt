package com.beanz.censusviz.repos

import com.beanz.censusviz.records.DQuery
import org.springframework.data.repository.CrudRepository
import javax.persistence.Table

@Table(name = "saved_queries")
interface SavedQueriesRepo: CrudRepository<DQuery, Int> {

    fun getAllByUid(uid: Int): List<DQuery>

}