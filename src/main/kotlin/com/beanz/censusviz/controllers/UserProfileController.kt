package com.beanz.censusviz.controllers

import com.beanz.censusviz.records.DLoginToken
import com.beanz.censusviz.records.DUserProfile
import com.beanz.censusviz.records.UserLoginDTO
import com.beanz.censusviz.records.UserRegisterDTO
import com.beanz.censusviz.repos.LoginTokenRepo
import com.beanz.censusviz.repos.UserProfileRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.security.MessageDigest
import java.util.*

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = ["http://localhost:3000"])
class UserProfileController(
        @Autowired
        private val userRepo: UserProfileRepo,
        @Autowired
        private val loginTokens: LoginTokenRepo
) {

    private val md = MessageDigest.getInstance("SHA-256")
    private val b64e = Base64.getEncoder()

    @PostMapping("/register", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun register(@RequestBody registerRequest: UserRegisterDTO): String {
        // password salt + hash
        val salt = UUID.randomUUID().toString().substring(0..15)
        val hashed = md.digest((registerRequest.password + salt).toByteArray(Charsets.UTF_8))
        val encoded = b64e.encode(hashed).toString(Charsets.UTF_8)

        val record = DUserProfile(
                id = null,
                username = registerRequest.username,
                first_name = registerRequest.first_name,
                last_name = registerRequest.last_name,
                password_hash = encoded,
                num_queries = 0,
                salt = salt
        )

        try {
            userRepo.save(record)
        } catch (e: Exception) {
            return "{ \"success\": false }"
        }
        return "{ \"success\": true }"
    }

    @PostMapping("/login", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun login(@RequestBody loginRequest: UserLoginDTO): String {
        // get salt by username
        val record = userRepo.findByUsername(loginRequest.username)
                ?: return "{ \"token\": null }"

        val salt = record.salt
        val hashed = md.digest((loginRequest.password + salt).toByteArray(Charsets.UTF_8))
        val encoded = b64e.encode(hashed).toString(Charsets.UTF_8)

        if(encoded != record.password_hash) {
            return "{ \"token\": null }"
        }

        // create new login token
        // delete existing
        loginTokens.deleteAllByUsername(record.username)
        val newToken = UUID.randomUUID().toString()
        loginTokens.save(DLoginToken(newToken, record.username))
        return """
            {
                "token": "$newToken",
                "first_name": "${record.first_name}"
                "last_name": "${record.last_name}"
            }
        """.trimIndent()
    }
}