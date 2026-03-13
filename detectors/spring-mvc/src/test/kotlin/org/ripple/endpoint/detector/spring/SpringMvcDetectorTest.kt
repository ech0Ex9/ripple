package org.ripple.endpoint.detector.spring

import org.ripple.endpoint.detector.api.model.EntryType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class SpringMvcDetectorTest {
    
    private val detector = SpringMvcDetector()
    
    @Test
    fun `should detect GetMapping endpoint`() {
        val sourceCode = """
            package com.example.controller;
            
            import org.springframework.web.bind.annotation.*;
            
            @RestController
            @RequestMapping("/api/users")
            public class UserController {
                
                @GetMapping("/{id}")
                public User getUser(@PathVariable String id) {
                    return userService.findById(id);
                }
            }
        """.trimIndent()
        
        val results = detector.detect(sourceCode, "src/main/java/com/example/controller/UserController.java", detector.defaultConfig)
        
        assertTrue(results.isNotEmpty())
        assertEquals(EntryType.HTTP, results[0].type)
        assertTrue(results[0].path?.contains("GET") == true)
    }
    
    @Test
    fun `should detect PostMapping endpoint`() {
        val sourceCode = """
            @PostMapping("/create")
            public User createUser(@RequestBody User user) {
                return userService.create(user);
            }
        """.trimIndent()
        
        val results = detector.detect(sourceCode, "controller/UserController.java", detector.defaultConfig)
        
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].path?.contains("POST") == true)
    }
    
    @Test
    fun `should combine class and method paths`() {
        val sourceCode = """
            @RestController
            @RequestMapping("/api/v1")
            public class ApiController {
                @GetMapping("/users")
                public List<User> getUsers() { return users; }
            }
        """.trimIndent()
        
        val results = detector.detect(sourceCode, "controller/ApiController.java", detector.defaultConfig)
        
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].path?.contains("/api/v1") == true)
    }
    
    @Test
    fun `should not detect non-Java files`() {
        val results = detector.detect("some code", "test.txt", detector.defaultConfig)
        assertTrue(results.isEmpty())
    }
    
    @Test
    fun `should check if file is applicable`() {
        assertTrue(detector.isApplicable("src/main/java/com/example/controller/UserController.java", detector.defaultConfig))
        assertTrue(detector.isApplicable("src/main/java/com/example/api/OrderApi.java", detector.defaultConfig))
    }
}