package com.beanz.censusviz.repos

import com.beanz.censusviz.records.DLoginToken
import org.springframework.data.repository.CrudRepository
import javax.persistence.Table

@Table(name = "login_tokens")
interface LoginTokenRepo: CrudRepository<DLoginToken, String> {

    fun deleteAllByUsername(username: String): Int

    fun findByToken(token: String): DLoginToken?

}