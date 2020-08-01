package com.beanz.censusviz.repos

import com.beanz.censusviz.records.DFriend
import com.beanz.censusviz.records.DFriendId
import org.springframework.data.repository.CrudRepository
import javax.persistence.Table

@Table(name = "user_friends")
interface FriendProfileRepo : CrudRepository<DFriend, DFriendId> {

    fun findByFollowee(followee: Int): List<DFriend>

    fun findByFollowerAndFollowee(follower: Int, followee: Int): DFriend?

}