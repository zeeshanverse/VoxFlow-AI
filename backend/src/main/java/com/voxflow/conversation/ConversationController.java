package com.voxflow.conversation;

import com.voxflow.common.CurrentUser;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/conversations")
public class ConversationController {
    private final ConversationRepository repo; private final CurrentUser current;
    public ConversationController(ConversationRepository repo,CurrentUser current){this.repo=repo;this.current=current;}
    @GetMapping public List<Summary> all(){
        return repo.findByUserIdOrderByCreatedAtDesc(current.get().getId()).stream().map(c->new Summary(c.getId(),c.getTitle(),c.getMessages().size())).toList();
    }
    @GetMapping("/{id}") public Detail one(@PathVariable Long id){
        var c=repo.findByIdAndUserId(id,current.get().getId()).orElseThrow();
        return new Detail(c.getId(),c.getTitle(),c.getMessages().stream().map(m->new MessageView(m.getRole(),m.getContent(),m.getCreatedAt())).toList());
    }
    public record Summary(Long id,String title,int messageCount){}
    public record Detail(Long id,String title,List<MessageView> messages){}
    public record MessageView(String role,String content,java.time.Instant createdAt){}
}
