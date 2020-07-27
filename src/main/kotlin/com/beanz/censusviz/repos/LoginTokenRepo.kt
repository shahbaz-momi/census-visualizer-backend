package com.beanz.censusviz.repos

import com.beanz.censusviz.records.DLoginToken
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional
import javax.persistence.Table

@Table(name = "login_tokens")
interface LoginTokenRepo: CrudRepository<DLoginToken, String> {

    @Transactional
    fun deleteAllByUsername(username: String): Int

    fun findByToken(token: String): DLoginToken?

}