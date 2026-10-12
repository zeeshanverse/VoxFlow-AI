package com.voxflow.assistant;

import com.voxflow.common.CurrentUser;
import com.voxflow.assistant.security.OperationCancellationService;
import com.voxflow.conversation.*;
import com.voxflow.llm.LlmProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class AssistantService {
    private final IntentRouter router; private final LlmProvider llm; private final CurrentUser current; private final ConversationRepository conversations; private final OperationCancellationService cancellations;
    public AssistantService(IntentRouter router,LlmProvider llm,CurrentUser current,ConversationRepository conversations,OperationCancellationService cancellations){
        this.router=router;this.llm=llm;this.current=current;this.conversations=conversations;this.cancellations=cancellations;
    }

    @Transactional 
    public Response respond(String text,Long conversationId,String operationId){
        if(text==null || text.isBlank()) throw new IllegalArgumentException("No speech was detected");
        var user=current.get();
        cancellations.throwIfCancelled(user.getEmail(), operationId);
        Conversation c;
        if(conversationId!=null) c=conversations.findByIdAndUserId(conversationId,user.getId()).orElseThrow();
        else c=new Conversation(user,text.length()>50?text.substring(0,50):text);
        String intent=router.route(text);
        String reply=switch(intent){
            case "START_INTERVIEW" -> "Start a Java interview from the Interviews section. I can evaluate your spoken answers.";
            case "SHOW_HISTORY" -> "Your saved conversations are available in your conversation history.";
            case "CURRENT_TIME" -> "The current server time is "+LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            default -> {
                String context = buildContext(c, text);

                System.out.println(
                        "Assistant Gemini input length: "
                                + context.length()
                                + " characters"
                );

                yield llm.generate(
                        "You are VoxFlow, a concise voice assistant. Be helpful and conversational.",
                        context
                );
            }
        };
        cancellations.throwIfCancelled(user.getEmail(), operationId);
        c.addMessage(new Message("user",text)); c.addMessage(new Message("assistant",reply)); conversations.save(c);
        return new Response(c.getId(),intent,reply);
    }
    private String buildContext(Conversation c,String text){
        StringBuilder b=new StringBuilder("Recent conversation:\\n");
        c.getMessages().stream().skip(Math.max(0,c.getMessages().size()-8)).forEach(m->b.append(m.getRole()).append(": ").append(m.getContent()).append("\\n"));
        return b.append("user: ").append(text).toString();
    }
    public record Response(Long conversationId,String intent,String response){}
}
