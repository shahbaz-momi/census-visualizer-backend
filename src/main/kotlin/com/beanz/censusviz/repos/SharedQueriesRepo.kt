package com.beanz.censusviz.repos

import com.beanz.censusviz.records.DSharedQuery
import com.beanz.censusviz.records.SharedQueryId
import org.springframework.data.repository.CrudRepository
import javax.persistence.Table

@Table(name = "shared_queries")
interface SharedQueriesRepo: CrudRepository<DSharedQuery, SharedQueryId> {


}