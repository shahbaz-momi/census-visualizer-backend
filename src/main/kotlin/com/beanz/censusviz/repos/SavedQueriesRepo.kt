package com.beanz.censusviz.repos

import com.beanz.censusviz.records.DQuery
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.Table

@Table(name = "saved_queries")
interface SavedQueriesRepo: CrudRepository<DQuery, Int> {

    fun getAllByUid(uid: Int): List<DQuery>

    @Transactional
    fun deleteAllByUidAndQidIn(uid: Int, qid: List<Int>)

    fun existsByUidAndQid(uid: Int, qid: Int): Boolean
}