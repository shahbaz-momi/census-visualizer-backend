package com.beanz.censusviz.controllers

import com.beanz.censusviz.records.*
import com.beanz.censusviz.repos.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.google.gson.GsonBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.sql.Timestamp
import java.time.Instant
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = ["http://localhost:3000"])
class UserQueryController(
        @Autowired
        private val loginTokenRepo: LoginTokenRepo,
        @Autowired
        private val userProfileRepo: UserProfileRepo,
        @Autowired
        private val savedQueriesRepo: SavedQueriesRepo,
        @Autowired
        private val educationRepo: DatasetEducationRepo,
        @Autowired
        private val employmentRepo: DatasetEmploymentRepo,
        @Autowired
        private val incomesRepo: DatasetIncomesRepo,
        @Autowired
        private val populationRepo: DatasetPopulationRepo,
        @Autowired
        private val userRepo : UserProfileRepo,
        @Autowired
        private val friendProfileRepo: FriendProfileRepo
) {

    private val gson = GsonBuilder().create()

    private fun getUserFromToken(tokenString : String): DUserProfile? {
        val token = loginTokenRepo.findByToken(tokenString) ?: return null
        return userProfileRepo.findByUsername(token.username)
    }

    private fun toGeoJson(records: List<DDatasetCombinedRecord>): JsonNode {
        val f = JsonNodeFactory.instance

        fun toGeoJson(record: DDatasetCombinedRecord): JsonNode {
            val node = f.objectNode()
            node.put("type", "Feature")
            node.putObject("properties").apply {
                put("mag", record.count)
            }
            node.putObject("geometry").apply {
                put("type", "Point")
                putArray("coordinates").apply {
                    add(record.lon)
                    add(record.lat)
                }
            }
            return node
        }

        val node = f.objectNode()
        node.put("type", "FeatureCollection")
        node.putArray("features").apply {
            records.map { toGeoJson(it) }.forEach { add(it) }
        }
        return node
    }

    private fun processForQuery(query: QueryDTO): List<DDatasetCombinedRecord> {
        return when (query.dataset) {
            "education" -> {
                educationRepo.findAllByAgeInAndSexAndMetaIn(query.age, query.sex, query.params)
            }
            "employment" -> {
                employmentRepo.findAllByAgeInAndSexAndMetaIn(query.age, query.sex, query.params)
            }
            "income" -> {
                incomesRepo.findAllByAgeInAndSexAndMetaIn(query.age, query.sex, query.params)
            }
            "population" -> {
                populationRepo.findAllByAgeInAndSex(query.age, query.sex)
            }
            else -> {
                emptyList()
            }
        }
    }

    @PostMapping("/query", consumes = [MediaType.APPLICATION_JSON_VALUE],
            produces = [MediaType.APPLICATION_JSON_VALUE])
    fun query(@RequestBody queries: List<QueryDTO>, @RequestHeader("Authorization") auth: String): String {
        val tokenString = auth.substringAfter("Bearer ")
        // check token is valid
        loginTokenRepo.findByToken(tokenString)
                ?: return "{ \"success\": false }"

        val f = JsonNodeFactory.instance

        return queries
                .map { processForQuery(it) }
                .map { toGeoJson(it) }
                .fold(f.arrayNode()!!) { acc: ArrayNode, jsonNode: JsonNode -> acc.add(jsonNode); acc }
                .toPrettyString()
    }

    @PostMapping("/save_queries", consumes = [MediaType.APPLICATION_JSON_VALUE],
            produces = [MediaType.APPLICATION_JSON_VALUE])
    fun saveQueries(@RequestBody queries: List<SavedQueryDTO>, @RequestHeader("Authorization") auth: String, servletResponse: HttpServletResponse): String {
        val tokenString = auth.substringAfter("Bearer ")
        val user = getUserFromToken(tokenString)

        if (user == null) {
            servletResponse.status = 400
            return "{ \"success\": false }"
        }

        // create new queries for each entity
        savedQueriesRepo.saveAll(queries.map {
            DQuery(
                    qid = it.qid,
                    uid = user.uid!!,
                    query = gson.toJson(it),
                    last_updated = Timestamp.from(Instant.now())
            )
        })
        return "{ \"success\": true }"
    }

    @GetMapping("/saved_queries", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun savedQueries(@RequestHeader("Authorization") auth: String, servletResponse: HttpServletResponse): String {
        val tokenString = auth.substringAfter("Bearer ")
        val user = getUserFromToken(tokenString)

        if (user == null) {
            servletResponse.status = 400
            return "{ \"success\": false }"
        }

        val dq = savedQueriesRepo.getAllByUid(user.uid!!)
        val queries = dq.map {
            val dto = gson.fromJson(it.query, QueryDTO::class.java)
            SavedQueryDTO(it.qid!!, dto.dataset, dto.params, dto.age, dto.sex)
        }
        return gson.toJson(queries)
    }

    @PostMapping("/delete_queries", consumes = [MediaType.APPLICATION_JSON_VALUE],
            produces = [MediaType.APPLICATION_JSON_VALUE])
    fun deleteQueries(@RequestBody qids: List<Int>, @RequestHeader("Authorization") auth: String, servletResponse: HttpServletResponse): String {
        val tokenString = auth.substringAfter("Bearer ")
        val user = getUserFromToken(tokenString)

        if (user == null) {
            servletResponse.status = 400
            return "{ \"success\": false }"
        }

        savedQueriesRepo.deleteAllByUidAndQidIn(user.uid!!, qids)
        return "{ \"success\": true }"
    }

    @GetMapping("/find_profiles", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findProfiles(@RequestHeader("Authorization") @RequestParam(name = "input") input: String, auth: String, servletResponse: HttpServletResponse): String {
        val profiles = userProfileRepo.findAllByUsernameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrderByUsername(input, input, input, PageRequest.of(0, 25)).map {
            UserProfileDTO(username = it.username, firstName = it.firstName, lastName = it.lastName)
        }
        return gson.toJson(profiles)
    }

    @GetMapping("/friends", consumes = [MediaType.APPLICATION_JSON_VALUE],
            produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getFriends(@RequestHeader("Authorization") auth: String, servletResponse: HttpServletResponse): String {
        val tokenString = auth.substringAfter("Bearer ")
        val user = getUserFromToken(tokenString)

        if (user == null) {
            servletResponse.status = 400
            return "{ \"success\": false }"
        }
        val uids = friendProfileRepo.findByFollowee(user.uid!!)

        val friendUsernames = mutableListOf<String>()
        for (uid in uids) {
            val username = userProfileRepo.findByUid(uid.follower)?.username
            if (username != null) {
                friendUsernames.add(username)
            }
        }

        return friendUsernames.toString()
    }

    @PostMapping("/friends", consumes = [MediaType.APPLICATION_JSON_VALUE],
            produces = [MediaType.APPLICATION_JSON_VALUE])
    fun addFriend(@RequestBody username: UsernameDTO, @RequestHeader("Authorization") auth: String, servletResponse: HttpServletResponse): String {
        val tokenString = auth.substringAfter("Bearer ")
        val user = getUserFromToken(tokenString)

        if (user == null) {
            servletResponse.status = 400
            return "{ \"success\": false }"
        }

        val friend = userProfileRepo.findByUsername(username.username)

        if (friend == null) {
            servletResponse.status = 400
            return "{ \"success\": false }"
        }

        if (user.uid == null || friend.uid == null) {
            servletResponse.status = 400
            return "{ \"success\": false }"
        }

        val record = DFriend(follower = user.uid, followee = friend.uid)

        // they are already friends
        friendProfileRepo.findByFollowerAndFollowee(record.follower, record.followee) ?: return "{ \"success\": true }"

        try {
            friendProfileRepo.save(record)
        } catch (e: Exception) {
            e.printStackTrace()
            servletResponse.status = 400
            return "{ \"success\": false }"
        }
        return "{ \"success\": true }"
    }
}

