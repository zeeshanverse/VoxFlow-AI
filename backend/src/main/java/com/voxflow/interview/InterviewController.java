package com.voxflow.interview;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/api/interviews")
public class InterviewController {
    private final InterviewService service;
    public InterviewController(InterviewService service){this.service=service;}
    @PostMapping public InterviewService.Start start(@RequestParam(defaultValue="Java Backend") String topic){return service.start(topic);}
    @PostMapping(value="/{id}/answer",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public InterviewService.AnswerResult answer(@PathVariable Long id,@RequestPart("audio") MultipartFile audio){return service.answer(id,audio);}
    @GetMapping("/{id}") public InterviewService.SessionResult get(@PathVariable Long id){return service.get(id);}
}
