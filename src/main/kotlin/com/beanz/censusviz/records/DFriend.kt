package com.beanz.censusviz.records

import java.io.Serializable
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.Table

@Entity
@IdClass(DFriendId::class)
@Table(name = "user_friends")
data class DFriend(
        @Id
        val follower: Int,
        @Id
        val followee: Int
)

data class DFriendId(
        val follower: Int = -1,
        val followee: Int = -1
) : Serializable

