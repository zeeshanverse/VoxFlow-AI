package com.voxflow.assistant;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestHeader;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/assistant")
public class AssistantController {
    private final AssistantService service;
    public AssistantController(AssistantService service){this.service=service;}
    @PostMapping("/message")
    public AssistantService.Response message(
            @Valid @RequestBody Request r,
            @RequestHeader(value = "X-VoxFlow-Operation-Id", required = false) String operationId
    ){
        return service.respond(r.text(), r.conversationId(), operationId);
    }
    public record Request(@NotBlank String text,Long conversationId){}
}
