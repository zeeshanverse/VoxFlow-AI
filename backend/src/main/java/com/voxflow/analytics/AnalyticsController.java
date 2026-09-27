package com.voxflow.analytics;
import com.voxflow.common.CurrentUser;
import com.voxflow.conversation.ConversationRepository;
import com.voxflow.interview.InterviewSessionRepository;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/analytics")
public class AnalyticsController {
    private final CurrentUser current; private final ConversationRepository conversations; private final InterviewSessionRepository interviews;
    public AnalyticsController(CurrentUser current,ConversationRepository conversations,InterviewSessionRepository interviews){this.current=current;this.conversations=conversations;this.interviews=interviews;}
    @GetMapping public Map<String,Object> get(){
        var id=current.get().getId();
        var cs=conversations.findByUserIdOrderByCreatedAtDesc(id);
        var is=interviews.findByUserIdOrderByCreatedAtDesc(id);
        double avg=is.stream().filter(x->x.isCompleted()).mapToDouble(x->x.getScore()).average().orElse(0);
        return Map.of("conversations",cs.size(),"interviews",is.size(),"completedInterviews",is.stream().filter(x->x.isCompleted()).count(),"averageInterviewScore",avg);
    }
}
