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
import java.awt.Color
import java.sql.Timestamp
import java.time.Instant
import javax.servlet.http.HttpServletResponse
import kotlin.random.Random

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
        private val friendProfileRepo: FriendProfileRepo,
        @Autowired
        private val sharedQueriesRepo: SharedQueriesRepo
) {

    private val gson = GsonBuilder().create()

    private fun getUserFromToken(tokenString : String): DUserProfile? {
        val token = loginTokenRepo.findByToken(tokenString) ?: return null
        return userProfileRepo.findByUsername(token.username)
    }

    private fun makeHeatmap(color: Color, maxMag: Int): String {
        val r = color.red
        val g = color.green
        val b = color.blue

        return """
            {
              "maxzoom": 9,
              "type": "heatmap",
              "paint": {
                "heatmap-weight": ["interpolate", ["linear"], ["get", "mag"], 0, 0, $maxMag, 1],
                "heatmap-intensity": 2,
                "heatmap-color": [
                  "interpolate",
                  ["linear"],
                  ["heatmap-density"],
                  0,
                  "rgba($r,$g,$b,0)",
                  0.2,
                  "rgba($r,$g,$b,0.2)",
                  0.4,
                  "rgba($r,$g,$b,0.4)",
                  0.6,
                  "rgba($r,$g,$b,0.6)",
                  0.8,
                  "rgba($r,$g,$b,0.8)",
                  0.9,
                  "rgba($r,$g,$b,1)"
                ],
                "heatmap-radius": ["interpolate", ["linear"], ["zoom"], 0, 10, 5, 15, 6, 30, 8, 50, 9, 100],
                "heatmap-opacity": ["interpolate", ["linear"], ["zoom"], 7, 1, 9, 1, 10, 1, 11, 1]
              }
            }
        """.trimIndent()
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

    @PostMapping("/query_by_id", consumes = [MediaType.APPLICATION_JSON_VALUE],
            produces = [MediaType.APPLICATION_JSON_VALUE])
    fun queryById(@RequestBody qids: List<Int>, @RequestHeader("Authorization") auth: String, servletResponse: HttpServletResponse): String {
        val tokenString = auth.substringAfter("Bearer ")
        val user = getUserFromToken(tokenString)

        if (user == null) {
            servletResponse.status = 400
            return "{ \"success\": false }"
        }

        val queries = savedQueriesRepo.findAllByUidAndQidIn(user.uid!!, qids)

        val f = JsonNodeFactory.instance

        return queries
                .map { gson.fromJson(it.query, QueryDTO::class.java) }
                .map { processForQuery(it) }
                .map { toGeoJson(it) }
                .fold(f.arrayNode()!!) { acc: ArrayNode, jsonNode: JsonNode -> acc.add(jsonNode); acc }
                .map { arr ->
                    """
                        {
                            "layer": ${arr.toPrettyString()},
                            "heatmap": ${makeHeatmap(Color.getHSBColor(Random.nextFloat(), 0.7f, 0.8f), 100000)},
                            "min": 0,
                            "max": 100000
                        }
                    """.trimIndent()
                }.joinToString(separator = ", ", prefix = "[", postfix = "]")
    }

    @GetMapping("/duplicate", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun duplicateQuery(@RequestParam("qid") qid: Int, @RequestHeader("Authorization") auth: String, servletResponse: HttpServletResponse): String {
        val tokenString = auth.substringAfter("Bearer ")
        val user = getUserFromToken(tokenString)

        if (user == null) {
            servletResponse.status = 400
            return "{ \"success\": false }"
        }

        val query = savedQueriesRepo.findById(qid).orElseGet { null }

        if(query == null) {
            servletResponse.status = 400
            return "{ \"success\": false }"
        }

        // duplicate and save our query with new uid and no qid
        savedQueriesRepo.save(query.copy(qid = null, uid = user.uid!!))
        return "{ \"success\": true }"
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

        if(!queries.mapNotNull { it.qid }.all {
            savedQueriesRepo.existsByUidAndQid(user.uid!!, it)
        }) {
            servletResponse.status = 400
            return "{ \"success\": false }"
        }

        // create new queries for each entity
        val mapped = queries.map {
            DQuery(
                    qid = it.qid,
                    uid = user.uid!!,
                    query = gson.toJson(it),
                    last_updated = Timestamp.from(Instant.now())
            )
        }
        savedQueriesRepo.saveAll(mapped)
        // return array of query IDs
        return gson.toJson(mapped.map { it.qid!! })
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

    @PostMapping("/share_queries", consumes = [MediaType.APPLICATION_JSON_VALUE],
            produces = [MediaType.APPLICATION_JSON_VALUE])
    fun shareQueries(@RequestBody shareRequests: List<QueryShareDTO>, @RequestHeader("Authorization") auth: String, servletResponse: HttpServletResponse): String {
        val tokenString = auth.substringAfter("Bearer ")
        val user = getUserFromToken(tokenString)
        if (user == null) {
            servletResponse.status = 400
            return "{ \"success\": false }"
        }
        shareRequests.map {
            userProfileRepo.findByUsername(it.username)?.uid to it
        }.filter {
            it.first != null &&
                    // user is friends with that mans
                    friendProfileRepo.findByFollowerAndFollowee(user.uid!!, it.first!!) != null
        }.forEach {
            sharedQueriesRepo.save(DSharedQuery(it.second.qid, it.first!!))
        }

        return "{ \"success\": true }"
    }

    @GetMapping("/shared_queries", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sharedQueries(@RequestParam("username") fromUser: String?, @RequestHeader("Authorization") auth: String, servletResponse: HttpServletResponse): String {
        val tokenString = auth.substringAfter("Bearer ")
        val user = getUserFromToken(tokenString)
        if (user == null) {
            servletResponse.status = 400
            return "{ \"success\": false }"
        }
        return if(fromUser != null) {
            gson.toJson(sharedQueriesRepo.getAllByUidByUsername(user.uid!!, fromUser).map {
                gson.fromJson(it.query, QueryDTO::class.java)
            })
        } else {
            gson.toJson(sharedQueriesRepo.getAllByUid(user.uid!!).map {
                gson.fromJson(it.query, QueryDTO::class.java)
            })
        }
    }

    @GetMapping("/find_profiles", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun findProfiles(@RequestHeader("Authorization") auth: String, @RequestParam(name = "input") input: String, servletResponse: HttpServletResponse): String {
        val tokenString = auth.substringAfter("Bearer ")
        val user = getUserFromToken(tokenString)

        if (user == null) {
            servletResponse.status = 400
            return "{ \"success\": false }"
        }
        val profiles = userProfileRepo.findAllByUsernameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrderByUsername(input, input, input, PageRequest.of(0, 25)).filter {
            it.uid != user.uid // filter out self from response
        }.map {
            UserProfileDTO(username = it.username, firstName = it.firstName, lastName = it.lastName)
        }
        return gson.toJson(profiles)
    }

    @GetMapping("/friends", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getFriends(@RequestHeader("Authorization") auth: String, servletResponse: HttpServletResponse): String {
        val tokenString = auth.substringAfter("Bearer ")
        val user = getUserFromToken(tokenString)

        if (user == null) {
            servletResponse.status = 400
            return "{ \"success\": false }"
        }

        return gson.toJson(friendProfileRepo.findByFollower(user.uid!!).mapNotNull {
            val usr = userProfileRepo.findByUid(it.followee)!!
            UserProfileDTO(username = usr.username, firstName = usr.firstName, lastName = usr.lastName)
        })
    }

    @PostMapping("/friends", consumes = [MediaType.APPLICATION_JSON_VALUE],
            produces = [MediaType.APPLICATION_JSON_VALUE])
    fun addFriend(@RequestBody friendRequests: List<String>, @RequestHeader("Authorization") auth: String, servletResponse: HttpServletResponse): String {
        val tokenString = auth.substringAfter("Bearer ")
        val user = getUserFromToken(tokenString)

        if (user == null) {
            servletResponse.status = 400
            return "{ \"success\": false }"
        }

        friendRequests.forEach {
            val friend = userProfileRepo.findByUsername(it)
            if (friend?.uid != null && user.uid != null && user.uid != friend.uid) { // disallow adding self
                try {
                    friendProfileRepo.save(DFriend(follower = user.uid, followee = friend.uid))
                } catch(e: Exception) {
                    e.printStackTrace()
                    servletResponse.status = 400
                }
            }
        }
        return if (servletResponse.status == 400) "{ \"success\": false }" else "{ \"success\": true }"
    }

    @GetMapping("/me", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getUser(@RequestHeader("Authorization") auth: String, servletResponse: HttpServletResponse): String{
        val tokenString = auth.substringAfter("Bearer ")
        val user = getUserFromToken(tokenString)

        if (user == null) {
            servletResponse.status = 400
            return "{ \"success\": false }"
        }
        return """
            {
                "first_name": "${user.firstName}",
                "last_name": "${user.lastName}",
                "user_name": "${user.username}",
                "num_queries": ${user.num_queries},
                "dark_mode": ${user.dark_mode}
            }
        """.trimIndent()
        return gson.toJson(user)
    }

    @PostMapping("/dark_mode", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun updateDarkMode(@RequestBody isDarkMode: Boolean, @RequestHeader("Authorization") auth: String, servletResponse: HttpServletResponse): String{
        val tokenString = auth.substringAfter("Bearer ")
        val user = getUserFromToken(tokenString)

        if (user == null) {
            servletResponse.status = 400
            return "{ \"success\": false }"
        }

        user.dark_mode = isDarkMode
        try {
            userProfileRepo.save(user)
        } catch (e: Exception) {
            e.printStackTrace()
            servletResponse.status = 400
            return "{ \"success\": false }"
        }
        return "{ \"success\": true }"
    }

    @GetMapping("/friend_queries", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getFriendQueries(@RequestParam(name = "username") username: String, @RequestHeader("Authorization") auth: String, servletResponse: HttpServletResponse): String{
        val tokenString = auth.substringAfter("Bearer ")
        val user = getUserFromToken(tokenString)

        if (user == null || user.username == username) {
            servletResponse.status = 400
            return "{ \"success\": false }"
        }

        val friend = userProfileRepo.findByUsername(username)!!

        // make sure user is friends
        if(friendProfileRepo.findByFollowerAndFollowee(user.uid!!, friend.uid!!) == null){
            servletResponse.status = 400
            return "{ \"success\": false }"
        }


        val queries = savedQueriesRepo.getAllByUid(friend.uid).map {
            val dto = gson.fromJson(it.query, QueryDTO::class.java)
            SavedQueryDTO(it.qid!!, dto.dataset, dto.params, dto.age, dto.sex)
        }
        return gson.toJson(queries)
    }
}

