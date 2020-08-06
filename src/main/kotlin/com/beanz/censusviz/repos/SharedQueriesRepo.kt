package com.beanz.censusviz.repos

import com.beanz.censusviz.records.DQuery
import com.beanz.censusviz.records.DSharedQuery
import com.beanz.censusviz.records.SharedQueryId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import javax.persistence.Table

@Table(name = "shared_queries")
interface SharedQueriesRepo: CrudRepository<DSharedQuery, SharedQueryId> {

    @Query("SELECT q.qid, q.uid, q.last_updated, q.query FROM DSharedQuery s JOIN DQuery q ON s.qid = q.qid AND s.uid = :uid")
    fun getAllByUid(@Param("uid") uid: Int): List<DQuery>

    @Query("SELECT q.qid, q.uid, q.last_updated, q.query FROM DSharedQuery s, DUserProfile u JOIN DQuery q ON s.qid = q.qid AND s.uid = :uid AND u.uid = q.uid AND u.username = :username")
    fun getAllByUidByUsername(@Param("uid") uid: Int, @Param("username") sharedByUsername: String): List<DQuery>
}