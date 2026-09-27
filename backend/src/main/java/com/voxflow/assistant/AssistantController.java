package com.voxflow.assistant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/assistant")
public class AssistantController {
    private final AssistantService service;
    public AssistantController(AssistantService service){this.service=service;}
    @PostMapping("/message")
    public AssistantService.Response message(@Valid @RequestBody Request r){return service.respond(r.text(),r.conversationId());}
    public record Request(@NotBlank String text,Long conversationId){}
}
