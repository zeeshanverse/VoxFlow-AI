package com.voxflow.common;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String,String>> bad(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String,String>> state(IllegalStateException e){return ResponseEntity.status(502).body(Map.of("error",e.getMessage()));}
}
