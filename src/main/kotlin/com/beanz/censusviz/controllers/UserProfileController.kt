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
import javax.servlet.http.HttpServletResponse

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
    fun register(@RequestBody registerRequest: UserRegisterDTO, servletResponse: HttpServletResponse): String {
        // password salt + hash
        val salt = UUID.randomUUID().toString().substring(0..15)
        val hashed = md.digest((registerRequest.password + salt).toByteArray(Charsets.UTF_8))
        val encoded = b64e.encode(hashed).toString(Charsets.UTF_8)

        val record = DUserProfile(
                uid = null,
                username = registerRequest.username,
                firstName = registerRequest.first_name,
                lastName = registerRequest.last_name,
                password_hash = encoded,
                num_queries = 0,
                salt = salt,
                dark_mode = false
        )

        try {
            userRepo.save(record)
        } catch (e: Exception) {
            e.printStackTrace()
            servletResponse.status = 400
            return "{ \"success\": false }"
        }
        return "{ \"success\": true }"
    }

    @PostMapping("/login", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun login(@RequestBody loginRequest: UserLoginDTO, response: HttpServletResponse): String {
        // get salt by username
        val record = userRepo.findByUsername(loginRequest.username)

        if(record == null) {
            response.status = 403
            return "{ \"token\": null }"
        }

        val salt = record.salt
        val hashed = md.digest((loginRequest.password + salt).toByteArray(Charsets.UTF_8))
        val encoded = b64e.encode(hashed).toString(Charsets.UTF_8)

        if(encoded != record.password_hash) {
            response.status = 403
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
                "first_name": "${record.firstName}",
                "last_name": "${record.lastName}"
            }
        """.trimIndent()
    }
}