package com.beanz.censusviz.records

import java.io.Serializable
import javax.persistence.*

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
        val follower: Int,
        val followee: Int
) : Serializable

