package com.demo.controller;

import co.elastic.apm.api.ElasticApm;
import com.demo.model.User;
import com.demo.repository.UserRepository;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static net.logstash.logback.argument.StructuredArguments.kv;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<User> getAllUsers() {
        MDC.put("action", "GET_ALL_USERS");
        try {
            log.info("Fetching all users");
            long start = System.currentTimeMillis();
            List<User> users = userRepository.findAll();
            log.info("Users fetched",
                    kv("userCount", users.size()),
                    kv("durationMs", System.currentTimeMillis() - start)
            );
            return users;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        MDC.put("action", "GET_USER_BY_ID");
        MDC.put("userId", String.valueOf(id));
        try {
            String traceId = ElasticApm.currentTransaction().getTraceId();
            log.info("Fetching user", kv("userId", id));
            return userRepository.findById(id)
                    .map(user -> {
                        log.info("User found",
                                kv("userId", user.getId()),
                                kv("userName", user.getName()),
                                kv("userAge", user.getAge()),
                                kv("result", "FOUND")
                        );
                        return ResponseEntity.ok()
                                .header("X-Trace-Id", traceId)
                                .body(user);
                    })
                    .orElseGet(() -> {
                        log.warn("User not found", kv("userId", id), kv("result", "NOT_FOUND"));
                        return ResponseEntity.notFound().build();
                    });
        } finally {
            MDC.clear();
        }
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        MDC.put("action", "CREATE_USER");
        try {
            String traceId = ElasticApm.currentTransaction().getTraceId();
            log.info("Creating user", kv("requestName", user.getName()), kv("requestAge", user.getAge()));
            User saved = userRepository.save(user);
            log.info("User created", kv("userId", saved.getId()), kv("result", "SUCCESS"));
            return ResponseEntity.ok().header("X-Trace-Id", traceId).body(saved);
        } finally {
            MDC.clear();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User body) {
        MDC.put("action", "UPDATE_USER");
        MDC.put("userId", String.valueOf(id));
        try {
            String traceId = ElasticApm.currentTransaction().getTraceId();
            log.info("Updating user", kv("userId", id));
            return userRepository.findById(id)
                    .map(existing -> {
                        existing.setName(body.getName());
                        existing.setAge(body.getAge());
                        User updated = userRepository.save(existing);
                        log.info("User updated",
                                kv("userId", updated.getId()),
                                kv("newName", updated.getName()),
                                kv("newAge", updated.getAge()),
                                kv("result", "UPDATED")
                        );
                        return ResponseEntity.ok()
                                .header("X-Trace-Id", traceId)
                                .body(updated);
                    })
                    .orElseGet(() -> {
                        log.warn("Update failed - not found", kv("userId", id), kv("result", "NOT_FOUND"));
                        return ResponseEntity.notFound().build();
                    });
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        MDC.put("action", "DELETE_USER");
        MDC.put("userId", String.valueOf(id));
        try {
            log.info("Deleting user", kv("userId", id));
            if (!userRepository.existsById(id)) {
                log.warn("Delete failed - not found", kv("userId", id), kv("result", "NOT_FOUND"));
                return ResponseEntity.notFound().build();
            }
            userRepository.deleteById(id);
            log.info("User deleted", kv("userId", id), kv("result", "DELETED"));
            return ResponseEntity.noContent().build();
        } finally {
            MDC.clear();
        }
    }

    // Test error — APM sẽ capture exception này
    @GetMapping("/error")
    public String testError() {
        MDC.put("action", "TEST_ERROR");
        try {
            log.info("API /api/users/error called");
            int x = 1 / 0;
            return "This will never return";
        } finally {
            MDC.clear();
        }
    }

    // Test slow — APM sẽ hiển thị transaction chậm
    @GetMapping("/slow")
    public List<User> slowApi() throws InterruptedException {
        MDC.put("action", "SLOW_API");
        try {
            log.info("API /api/users/slow called - sleeping 3s");
            Thread.sleep(3000);
            List<User> users = userRepository.findAll();
            log.info("Slow API done", kv("userCount", users.size()));
            return users;
        } finally {
            MDC.clear();
        }
    }
}
