package com.showmethemoney.system.interfaces;

import com.showmethemoney.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "UP", "db", "connected")));
        } catch (Exception e) {
            return ResponseEntity.status(503)
                    .body(ApiResponse.ok(Map.of("status", "DOWN", "db", "disconnected")));
        }
    }

    // DB 등 외부 의존성 체크 없이 프로세스 생존만 확인. k8s livenessProbe 전용 —
    // /api/health(DB 체크 포함)를 liveness에 쓰면 DB 장애 시 정상 프로세스가 재시작 루프에 빠짐.
    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<Map<String, String>>> ping() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "UP")));
    }
}
