package com.beanz.censusviz.repos

import com.beanz.censusviz.records.DFriend
import org.springframework.data.repository.CrudRepository
import javax.persistence.Table

@Table(name = "user_friends")
interface FriendProfileRepo : CrudRepository<DFriend, Int> {

    fun findByFollowee(followee: Int): List<DFriend>

}