package com.example.cloudcomputing

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.data.redis.core.StringRedisTemplate

/*
@RestController
class RedisTestController(
    private val redisTemplate: StringRedisTemplate
) {

    @GetMapping("/redis/set")
    fun set(
        @RequestParam key: String,
        @RequestParam value: String
    ): String {
        redisTemplate.opsForValue().set(key, value)
        return "OK, uloženo $key=$value"
    }

    @GetMapping("/redis/get")
    fun get(@RequestParam key: String): String {
        return redisTemplate.opsForValue().get(key) ?: "null (nenalezeno)"
    }
}
*/
@RestController
class RedisTestController(
    private val redisTemplate: StringRedisTemplate
) {

    @GetMapping("/redis/set")
    fun set(
        @RequestParam key: String,
        @RequestParam value: String
    ): String {
        return try {
            redisTemplate.opsForValue().set(key, value)
            "OK, uloženo $key=$value"
        } catch (e: Exception) {
            e.printStackTrace()  // uvidíš stacktrace v IntelliJ konzoli
            "ERROR: ${e.javaClass.simpleName}: ${e.message}"
        }
    }

    @GetMapping("/redis/get")
    fun get(@RequestParam key: String): String {
        return try {
            redisTemplate.opsForValue().get(key) ?: "null (nenalezeno)"
        } catch (e: Exception) {
            e.printStackTrace()
            "ERROR: ${e.javaClass.simpleName}: ${e.message}"
        }
    }
}