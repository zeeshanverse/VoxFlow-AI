package com.voxflow.health;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/api/health")
    public Map<String,Object> health(){return Map.of("status","UP","service","voxflow-assistant","time",Instant.now().toString());}
}
